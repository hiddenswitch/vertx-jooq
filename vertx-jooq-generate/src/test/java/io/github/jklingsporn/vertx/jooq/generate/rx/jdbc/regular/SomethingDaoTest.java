package io.github.jklingsporn.vertx.jooq.generate.rx.jdbc.regular;

import generated.rx.jdbc.regular.vertx.Tables;
import generated.rx.jdbc.regular.vertx.tables.daos.SomethingDao;
import generated.rx.jdbc.regular.vertx.tables.pojos.Something;
import io.github.jklingsporn.vertx.jooq.generate.HsqldbConfigurationProvider;
import io.github.jklingsporn.vertx.jooq.generate.rx.RXTestBase;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.jooq.Condition;
import org.junit.BeforeClass;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Created by jensklingsporn on 02.11.16.
 */
public class SomethingDaoTest extends RXTestBase<Something, Integer, Long, SomethingDao> {

    public SomethingDaoTest() {
        super(Tables.SOMETHING.SOMEHUGENUMBER, new SomethingDao(HsqldbConfigurationProvider.getInstance().createDAOConfiguration(), Vertx.vertx(new VertxOptions()
            .setEventLoopPoolSize(16)
            .setWorkerPoolSize(32)
        .setInternalBlockingPoolSize(64))));
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        HsqldbConfigurationProvider.getInstance().setupDatabase();
    }

    @Override
    protected Something create() {
        return createWithId().setSomeid(null);
    }

    @Override
    protected Something createWithId() {
        Random random = new Random();
        Something something = new Something();
        something.setSomeid(random.nextInt());
        something.setSomedouble(random.nextDouble());
        something.setSomehugenumber(random.nextLong());
        something.setSomejsonarray(new JsonArray().add(1).add(2).add(3));
        something.setSomejsonobject(new JsonObject().put("key", "value"));
        something.setSomesmallnumber((short) random.nextInt(Short.MAX_VALUE));
        //someBoolean has a default value and does not need to be set
        something.setSomestring("my_string");
        something.setSometimestamp(LocalDateTime.now());
        return something;
    }

    @Override
    protected Something setId(Something pojo, Integer id) {
        return pojo.setSomeid(id);
    }

    @Override
    protected Something setSomeO(Something pojo, Long someO) {
        return pojo.setSomehugenumber(someO);
    }

    @Override
    protected Integer getId(Something pojo) {
        return pojo.getSomeid();
    }

    @Override
    protected Long createSomeO() {
        return new Random().nextLong();
    }

    @Override
    protected Condition eqPrimaryKey(Integer id) {
        return Tables.SOMETHING.SOMEID.eq(id);
    }

    @Override
    protected void assertDuplicateKeyException(Throwable x) {
        //CompletionException -> DataAccessException -> SQLIntegrityConstraintViolationException
        assertException(SQLIntegrityConstraintViolationException.class, x);
    }
}
