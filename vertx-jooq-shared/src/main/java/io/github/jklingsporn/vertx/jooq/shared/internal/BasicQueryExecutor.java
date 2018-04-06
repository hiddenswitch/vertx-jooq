package io.github.jklingsporn.vertx.jooq.shared.internal;

import org.jooq.*;

import java.util.function.Function;

/**
 * @param <EXECUTE> the result type returned for insert, update and delete-operations. This varies on the VertxDAO-subtypes, e.g. {@code Future<Integer>}.
 */
public interface BasicQueryExecutor<EXECUTE>{


    /**
     * Executes a query and returns the result of the execution (usually an <code>Integer</code>-value)
     * @param queryFunction
     * @return the result type returned for all insert, update and delete-operations.
     * @see Query#execute()
     */
    EXECUTE execute(Function<DSLContext, ? extends Query> queryFunction);


}