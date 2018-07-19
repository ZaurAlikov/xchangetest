import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.num.BigDecimalNum;
import org.ta4j.core.num.Num;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class StrategyTests {

    public static void rsiEmaStrategyTests(TimeSeries series) {

        for (int i = 20; i <= 40; i+=5){
            startTests(series, i, i+4);
        }
    }

    private static Num analytics(TradingRecord tradingRecord) {
        List<Trade> trades = tradingRecord.getTrades();
        Num sum = BigDecimalNum.valueOf("15");
        for (Trade trade : trades){
            Num priceEntry = trade.getEntry().getPrice();
            Num priceExit = trade.getExit().getPrice();
            if (priceExit.isGreaterThan(priceEntry)) {
                sum = sum.plus(sum.multipliedBy(BigDecimalNum.valueOf("0.009")));
            } else {
                sum = sum.minus(sum.multipliedBy(BigDecimalNum.valueOf("0.021")));
            }
        }
        return sum;
    }

    private static void startTests(TimeSeries series, int rsiLow1, int rsiLow2){
        new Thread(() -> {
            Strategy strategy;
            TimeSeriesManager seriesManager;
            TradingRecord tradingRecord;
            for(int rsiLow = rsiLow1; rsiLow <= rsiLow2; rsiLow++){
                for(int rsiHigh = 60; rsiHigh <= 80; rsiHigh++){
                    for(int rsiPeriod = 5; rsiPeriod <= 14; rsiPeriod++){
                        for(int shortEmaPeriod = 5; shortEmaPeriod <= 14; shortEmaPeriod++){
                            for(int longEmaPeriod = 14; longEmaPeriod <= 32; longEmaPeriod++){
                                strategy = Strategies.getRsiEmaStrategy(series, rsiLow, rsiHigh, rsiPeriod, shortEmaPeriod, longEmaPeriod);
                                seriesManager = new TimeSeriesManager(series);
                                tradingRecord = seriesManager.run(strategy);
                                if(analytics(tradingRecord).isGreaterThan(BigDecimalNum.valueOf(16))){
                                    System.out.println(analytics(tradingRecord)+","+rsiLow+","+rsiHigh+","+rsiPeriod+","+shortEmaPeriod+","+longEmaPeriod);
                                    fileWriter(analytics(tradingRecord)+","+rsiLow+","+rsiHigh+","+rsiPeriod+","+shortEmaPeriod+","+longEmaPeriod+"\r\n", "diff"+Thread.currentThread().getName()+".txt");
                                }
                            }
                        }
                    }
                }
            }

        }).start();
    }

    private static void fileWriter(String text, String fileName) {
        try(FileWriter writer = new FileWriter(fileName, true))
        {
            writer.write(text);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
