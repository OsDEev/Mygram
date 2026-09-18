package org.telegram.messenger.plugins;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EvaluatorException;

final class PluginRuntime {

    static final int MAX_SCRIPT_SIZE = 512 * 1024;
    static final int MAX_ENTRY_SIZE = 2 * 1024 * 1024;
    static final int MAX_ARCHIVE_SIZE = 8 * 1024 * 1024;
    static final int MAX_ARCHIVE_ENTRIES = 256;

    static final int BUDGET_EVALUATE = 20_000_000;
    static final int BUDGET_HOOK = 400_000;
    static final int BUDGET_HTTP = 50_000;
    static final int BUDGET_HELPERS = 5_000_000;

    static final ExecutorService HOOK_EXECUTOR = Executors.newSingleThreadExecutor(PluginRuntime::newDaemonThread);
    static final ExecutorService NETWORK_EXECUTOR = Executors.newFixedThreadPool(2, PluginRuntime::newDaemonThread);
    static final ExecutorService EXTRACT_EXECUTOR = Executors.newFixedThreadPool(2, PluginRuntime::newDaemonThread);

    private static final AtomicBoolean FACTORY_INSTALLED = new AtomicBoolean(false);
    private static final ThreadLocal<Budget> CURRENT_BUDGET = new ThreadLocal<>();

    private PluginRuntime() {}

    private static Thread newDaemonThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "mygram-plugin");
        thread.setDaemon(true);
        return thread;
    }

    static void ensureFactory() {
        if (FACTORY_INSTALLED.get()) {
            return;
        }
        synchronized (PluginRuntime.class) {
            if (FACTORY_INSTALLED.get()) {
                return;
            }
            try {
                ContextFactory.initGlobal(new PluginContextFactory());
            } catch (IllegalStateException ignored) {
            }
            FACTORY_INSTALLED.set(true);
        }
    }

    static void pushBudget(int remaining) {
        CURRENT_BUDGET.set(new Budget(remaining));
    }

    static void popBudget() {
        CURRENT_BUDGET.remove();
    }

    private static final class Budget {
        long remaining;

        Budget(int remaining) {
            this.remaining = remaining;
        }
    }

    static final class BudgetExceededException extends EvaluatorException {
        BudgetExceededException() {
            super("Plugin execution budget exceeded");
        }
    }

    private static final class PluginContextFactory extends ContextFactory {

        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setOptimizationLevel(-1);
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setInstructionObserverThreshold(10_000);
            return cx;
        }

        @Override
        protected void observeInstructionCount(Context cx, int instructionCount) {
            Budget budget = CURRENT_BUDGET.get();
            if (budget != null) {
                budget.remaining -= instructionCount;
                if (budget.remaining < 0) {
                    throw new BudgetExceededException();
                }
            }
        }
    }
}