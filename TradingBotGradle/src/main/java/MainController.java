import Configuration.TradingConfiguration;
import Configurations.Default.Strategy;
import BaseStrategy.BaseStrategy;
import Interfaces.IMarketData;
import Interfaces.IStrategy;
import Model.StopMode;
import Model.ThreadDetails;
import net.jacobpeterson.alpaca.AlpacaAPI;
import org.reflections.Reflections;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class MainController extends Thread{
    private IMarketData marketData;
    private AlpacaAPI alpacaAPI;
    private HashMap<String, ThreadDetails> runningThreads = new HashMap<>();



    public MainController(IMarketData marketData, AlpacaAPI alpacaAPI){
        this.marketData = marketData;
        this.alpacaAPI = alpacaAPI;
    }

    /**
     * starts MainController
     * creates Tradingbots as new Threads
     */
    public void run(){
        var watchlist = new Properties();
        var oldRunningThreads = new HashMap<String, ThreadDetails>();

        while (true) {
            refreshRunningThreads();

            if(oldRunningThreads != this.runningThreads){
                // Todo rebalance money
            }

            if(watchlist != ReadWatchlistFile.readWatchlistFile("trading.Watchlist.properties")){
                // Todo find good strategy and rebalance money
            }
        }
            // Todo Maincontroller should only reevaluate if something changed
            var strategies = updateClasses();
            for(int i = 1; i < watchlist.size() + 1; i++){
                var symbol = watchlist.getProperty(Integer.toString(i));
                if(this.runningThreads.containsKey(symbol)){
                    continue;
                }

                var strategy = getBestStrategy(strategies, symbol);

                if(strategy != null){
                    var riskFactor = 90 / watchlist.size();

                    System.out.print(symbol + " get's started with strategy: " + strategy.getClass().getName() + "\n");
                    var configuration = new TradingConfiguration(symbol, strategy, riskFactor); // TODO implement setuppservice
                    var thread = new ThreadDetails(new Thread(new TradingBot(configuration, new TradingbotAPI(configuration, alpacaAPI), this.marketData, this)));
                    thread.getThread().start();
                    this.runningThreads.put(symbol, thread);
                    System.out.println("Number of active threads : " + Thread.activeCount());
                }
                else{
                    System.out.println("Couldn't find good strategy for Symbol: " + symbol);
                }
            }
    }

    private HashMap<String, ThreadDetails> refreshRunningThreads() {
        ArrayList<String> toDelete = new ArrayList<>();
        this.runningThreads.forEach((symbol, threadDetails) -> {
            if(!threadDetails.getThread().isAlive()){
                System.out.println("Removed Thread " + symbol);
                toDelete.add(symbol);
            }
        });

        for(String symbolOfToDelete:toDelete){
            this.runningThreads.remove(symbolOfToDelete);
        }

        return this.runningThreads;
    }

    public StopMode getStopMode(String marktOfThread) throws Exception {
        if(this.runningThreads.containsKey(marktOfThread)){
            return this.runningThreads.get(marktOfThread).getStopMode();
        }

        throw new Exception("Can't find a Tradingbot working on market: " + marktOfThread);
        // handle Thread that isn't being tract
    }

    public void stop(StopMode stopMode, String marketToClose) throws Exception {
        if (marketToClose == null){
            for(ThreadDetails thread:this.runningThreads.values()){
                thread.setStopMode(stopMode);
            }
            return;
        }

        if(this.runningThreads.containsKey(marketToClose)){
            this.runningThreads.get(marketToClose).setStopMode(stopMode);
            return;
        }

        throw new Exception("Can't find a Tradingbot working on market: " + marketToClose);
    }


    /**
     * refreshes Strategy Classes and checks for scurity Key
     * @return all Strategys in this project as Class
     */
    private ArrayList<IStrategy> updateClasses(){
        Reflections reflections = new Reflections("Configurations");
        Set<Class<? extends BaseStrategy>> strategies = reflections.getSubTypesOf(BaseStrategy.class);
        strategies.add(Strategy.class);

        ClassLoader classLoader = MainController.class.getClassLoader();

        ArrayList<IStrategy> strategys = new ArrayList<>();

        for(Class<? extends BaseStrategy> strategy:strategies){
            try {
                strategys.add((IStrategy) classLoader.loadClass(strategy.getName()).newInstance());

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        strategys = checkSecretKey(strategys);

        return strategys;
    }

    /**
     * checks given Classes with their Scuritykey
     * @param strategies that need to get checked
     * @return the strategies given without the strategies
     */
    private ArrayList<IStrategy> checkSecretKey(ArrayList<IStrategy> strategies){
        try{
            var secretKeys = new ArrayList<Double>();
            for(IStrategy strategy:strategies){
                secretKeys.add(strategy.getSecretKey());
            }

            for(int i = 0; i < secretKeys.size(); i++){
                if(Collections.frequency(secretKeys, secretKeys.get(i)) > 1){
                    strategies.remove(strategies.get(i));
                    continue;
                }
                if(secretKeys.get(i) * 19 * 7 * 77 * 217 * 9 / 3 / 99 / 83 % 79 != 0){
                    strategies.remove(strategies.get(i));
                    continue;
                }
            }
            return strategies;
        }
        catch (NumberFormatException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * goes through given List of strategies and pics the best one for the given symbol
     * @param strategies all strategies that could be used as strategy
     * @param symbol the symbol of the market where the best strategy needs to be found
     * @return returns the strategy that should be the best one
     */
    private IStrategy getBestStrategy(List<IStrategy> strategies, String symbol){ // Todo implement that a Strategy that got "banned" from the user, won't get chosen for the same Market
        List<Double> strategyChance = new ArrayList<>();
        for(IStrategy strategy:strategies){
            var data = marketData.getMarketData(ZonedDateTime.now(ZoneId.of("America/New_York")).minusMinutes(strategy.getTimeFrameForEvaluation().getMinute()), ZonedDateTime.now(ZoneId.of("America/New_York")), symbol, strategy.getBarsTimeFrame());
            var profitLoss = strategy.evaluateChancesToBeSuccessful(data);
            System.out.println("Estimation of profit with " + strategy.getName() + ": " + profitLoss);
            strategyChance.add(profitLoss);
        }
        var bestChance = Collections.max(strategyChance);
        if(bestChance > 0){
            return strategies.get(strategyChance.indexOf(bestChance));
        }
        return null;
    }
}
