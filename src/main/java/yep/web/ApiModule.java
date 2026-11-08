package yep.web;

import yep.data.DataStore;
import yep.model.User;
import yep.service.YepServices;
import yep.util.YepException;

/** Base class for each group of API routes. Subclasses register their own routes. */
public abstract class ApiModule {
    protected final YepServices s;
    protected final DataStore store;
    protected final Views v;

    protected ApiModule(YepServices services, Views views) {
        this.s = services;
        this.store = services.store;
        this.v = views;
    }

    public abstract void register(Router router);

    /** Makes sure the logged-in user is an instance of the expected role class. */
    protected <T extends User> T as(Request req, Class<T> type) {
        User u = req.user();
        if (!type.isInstance(u)) {
            throw YepException.denied();
        }
        return type.cast(u);
    }
}
