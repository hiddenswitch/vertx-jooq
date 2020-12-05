package io.github.jklingsporn.vertx.jooq.rx.reactivepg;

import io.github.jklingsporn.vertx.jooq.rx.RXQueryExecutor;
import io.github.jklingsporn.vertx.jooq.shared.internal.QueryResult;
import io.github.jklingsporn.vertx.jooq.shared.reactive.AbstractReactiveQueryExecutor;
import io.github.jklingsporn.vertx.jooq.shared.reactive.ReactiveQueryExecutor;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.*;
import io.vertx.reactivex.sqlclient.Transaction;
import io.vertx.sqlclient.Row;
import org.jooq.*;
import org.jooq.Query;
import org.jooq.exception.TooManyRowsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by jensklingsporn on 01.03.18.
 */
public class ReactiveRXGenericQueryExecutor extends AbstractReactiveQueryExecutor implements ReactiveQueryExecutor<Single<List<Row>>,Single<Optional<Row>>,Single<Integer>>, RXQueryExecutor {

    protected final SqlClient delegate;
    protected final Transaction transaction;


    public ReactiveRXGenericQueryExecutor(Configuration configuration, SqlClient delegate) {
        this(configuration, delegate, null);
    }

    public ReactiveRXGenericQueryExecutor(Configuration configuration, SqlClient delegate, Transaction transaction) {
        super(configuration);
        this.delegate = delegate;
        this.transaction = transaction;
    }

    @Override
    public <Q extends Record> Single<List<Row>> findManyRow(Function<DSLContext, ? extends ResultQuery<Q>> queryFunction) {
        return executeAny(queryFunction).map(res ->
                StreamSupport
                .stream(rxGetDelegate(res).spliterator(), false)
                        .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    /**
     * for some reason getDelegate returns untyped version
     */
    io.vertx.sqlclient.RowSet<io.vertx.sqlclient.Row> rxGetDelegate(RowSet<io.vertx.reactivex.sqlclient.Row> res) {
        return res.getDelegate();
    }

    @Override
    public <Q extends Record> Single<Optional<Row>> findOneRow(Function<DSLContext, ? extends ResultQuery<Q>> queryFunction) {
        return executeAny(queryFunction).map(res-> {
            switch (res.size()) {
                case 0: return Optional.empty();
                case 1: return Optional.ofNullable(rxGetDelegate(res).iterator().next());
                default: throw new TooManyRowsException(String.format("Found more than one row: %d", res.size()));
            }
        });
    }

    @Override
    public Single<Integer> execute(Function<DSLContext, ? extends Query> queryFunction) {
        return executeAny(queryFunction).map(SqlResult::rowCount);
    }

    protected Tuple rxGetBindValues(Query query) {
        ArrayList<Object> bindValues = new ArrayList<>();
        for (Param<?> param : query.getParams().values()) {
            Object value = convertToDatabaseType(param);
            bindValues.add(value);
        }
        Tuple tuple = Tuple.tuple();
        bindValues.forEach(tuple::addValue);
        return tuple;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Record> Single<QueryResult> query(Function<DSLContext, ? extends ResultQuery<R>> queryFunction) {
        Query query = createQuery(queryFunction);
        log(query);
        Single<RowSet<io.vertx.reactivex.sqlclient.Row>> rowSingle = delegate.preparedQuery(toPreparedQuery(query)).rxExecute(rxGetBindValues(query));
        return rowSingle.map(RxReactiveQueryResult::new);
    }

    /**
     * @return an instance of a <code>ReactiveRXGenericQueryExecutor</code> that performs all CRUD
     * functions in the scope of a transaction. The transaction has to be committed/rolled back by calling <code>commit</code>
     * or <code>rollback</code> on the QueryExecutor returned.
     */
    public Single<? extends ReactiveRXGenericQueryExecutor> beginTransaction(){
        if(transaction != null){
            throw new IllegalStateException("Already in transaction");
        }

        Pool pool = (Pool) this.delegate;
        return pool.rxGetConnection()
            .flatMap(conn->conn.rxBegin().map(newInstance(conn)));
    }

    protected io.reactivex.functions.Function<Transaction, ? extends ReactiveRXGenericQueryExecutor> newInstance(SqlConnection conn) {
        return transaction -> new ReactiveRXGenericQueryExecutor(configuration(),conn,transaction);
    }

    /**
     * Commits a transaction.
     * @return a <code>Completable</code> that completes when the transaction has been committed.
     * @throws IllegalStateException if not called <code>beginTransaction</code> before.
     */
    public Completable commit(){
        if(transaction==null){
            throw new IllegalStateException("Not in transaction");
        }
        return transaction.rxCommit();
    }

    /**
     * Rolls a transaction back.
     * @return a <code>Completable</code> that completes when the transaction has been rolled back.
     * @throws IllegalStateException if not called <code>beginTransaction</code> before.
     */
    public Completable rollback(){
        if(transaction==null){
            throw new IllegalStateException("Not in transaction");
        }
        return transaction.rxRollback();
    }

    /**
     * Convenience method to perform multiple calls on a transactional QueryExecutor, committing the transaction and
     * returning a result.
     * @param transaction your code using a transactional QueryExecutor.
     *                    <pre>
     *                    {@code
     *                    ReactiveRXGenericQueryExecutor nonTransactionalQueryExecutor...;
     *                    Maybe<QueryResult> resultOfTransaction = nonTransactionalQueryExecutor.transaction(transactionalQueryExecutor ->
     *                      {
     *                          //make all calls on the provided QueryExecutor that runs all code in a transaction
     *                          return transactionalQueryExecutor.execute(dslContext -> dslContext.insertInto(Tables.XYZ)...)
     *                              .compose(i -> transactionalQueryExecutor.query(dslContext -> dslContext.selectFrom(Tables.XYZ).where(Tables.XYZ.SOME_VALUE.eq("FOO")));
     *                      }
     *                    );
     *                    }
     *                    </pre>
     * @param <U> the return type.
     * @return the result of the transaction.
     */
    public <U> Maybe<U> transaction(io.reactivex.functions.Function<ReactiveRXGenericQueryExecutor, Maybe<U>> transaction){
        return beginTransaction()
                .toMaybe()
                .flatMap(queryExecutor -> transaction.apply(queryExecutor) //perform user tasks
                        .flatMap(res -> queryExecutor.commit() //commit the transaction
                                .andThen(Maybe.just(res)))) //and return the result
                ;
    }

    @Override
    public void release() {
        if(delegate!=null){
            delegate.close();
        }
    }

    /**
     * Executes the given queryFunction and returns a <code>RowSet</code>
     * @param queryFunction the query to execute
     * @return the results, never null
     */
    public Single<RowSet<io.vertx.reactivex.sqlclient.Row>> executeAny(Function<DSLContext, ? extends Query> queryFunction) {
        Query query = createQuery(queryFunction);
        log(query);
        return delegate.preparedQuery(toPreparedQuery(query)).rxExecute(rxGetBindValues(query));
    }
}
