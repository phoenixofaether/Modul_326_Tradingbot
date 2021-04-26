package Model;

import Configuration.TradingConfiguration;

public class ThreadDetails {
    private Thread thread;
    private TradingConfiguration configuration;
    private StopMode stopMode;

    public ThreadDetails(Thread thread){
        this.thread = thread;
        this.stopMode = StopMode.running;
    }

    public ThreadDetails(Thread thread, StopMode stopMode){
        this.thread = thread;
        this.stopMode = stopMode;
    }

    public Thread getThread() {
        return thread;
    }

    public TradingConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(TradingConfiguration configuration) {
        this.configuration = configuration;
    }

    public StopMode getStopMode() {
        return stopMode;
    }

    public void setStopMode(StopMode stopMode) {
        this.stopMode = stopMode;
    }
}
