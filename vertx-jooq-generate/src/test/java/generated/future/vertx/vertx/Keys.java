/*
 * This file is generated by jOOQ.
*/
package generated.future.vertx.vertx;


import generated.future.vertx.vertx.tables.Something;
import generated.future.vertx.vertx.tables.Somethingcomposite;
import generated.future.vertx.vertx.tables.records.SomethingRecord;
import generated.future.vertx.vertx.tables.records.SomethingcompositeRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>VERTX</code> 
 * schema
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<SomethingRecord, Integer> IDENTITY_SOMETHING = Identities0.IDENTITY_SOMETHING;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<SomethingRecord> SYS_PK_10178 = UniqueKeys0.SYS_PK_10178;
    public static final UniqueKey<SomethingcompositeRecord> SYS_PK_10180 = UniqueKeys0.SYS_PK_10180;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 extends AbstractKeys {
        public static Identity<SomethingRecord, Integer> IDENTITY_SOMETHING = createIdentity(Something.SOMETHING, Something.SOMETHING.SOMEID);
    }

    private static class UniqueKeys0 extends AbstractKeys {
        public static final UniqueKey<SomethingRecord> SYS_PK_10178 = createUniqueKey(Something.SOMETHING, "SYS_PK_10178", Something.SOMETHING.SOMEID);
        public static final UniqueKey<SomethingcompositeRecord> SYS_PK_10180 = createUniqueKey(Somethingcomposite.SOMETHINGCOMPOSITE, "SYS_PK_10180", Somethingcomposite.SOMETHINGCOMPOSITE.SOMEID, Somethingcomposite.SOMETHINGCOMPOSITE.SOMESECONDID);
    }
}
