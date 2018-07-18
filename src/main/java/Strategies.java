import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TimeSeries;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.BigDecimalNum;
import org.ta4j.core.trading.rules.*;

public class Strategies {

    public static Strategy getRsiEmaStrategy(TimeSeries series, int rsiLow, int rsiHigh, int rsiPeriod, int shortEmaPeriod, int longEmaPeriod) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
//        int rsiLow = 40;
//        int rsiHigh = 60;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortEmaPeriod);
        EMAIndicator longEma = new EMAIndicator(closePrice, longEmaPeriod);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, rsiPeriod);
        PreviousValueIndicator rsiIndicator1 = new PreviousValueIndicator(rsiIndicator, 1);
        PreviousValueIndicator rsiIndicator2 = new PreviousValueIndicator(rsiIndicator, 2);
        PreviousValueIndicator rsiIndicator3 = new PreviousValueIndicator(rsiIndicator, 3);
        PreviousValueIndicator rsiIndicator4 = new PreviousValueIndicator(rsiIndicator, 4);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(rsiIndicator, BigDecimalNum.valueOf(rsiHigh)))
                .and(new UnderIndicatorRule(rsiIndicator1, BigDecimalNum.valueOf(rsiLow))
                        .or(new UnderIndicatorRule(rsiIndicator2, BigDecimalNum.valueOf(rsiLow))
                                .or(new UnderIndicatorRule(rsiIndicator3, BigDecimalNum.valueOf(rsiLow))
                                        .or(new UnderIndicatorRule(rsiIndicator4, BigDecimalNum.valueOf(rsiLow))))))
                .and(new OverIndicatorRule(rsiIndicator, rsiIndicator1))
                .and(new OverIndicatorRule(rsiIndicator1, rsiIndicator2))
                .and(new OverIndicatorRule(rsiIndicator2, rsiIndicator3));

        // Exit rule
        Rule exitRule = new StopLossRule(closePrice, BigDecimalNum.valueOf("2"))
                .or(new StopGainRule(closePrice, BigDecimalNum.valueOf("1")));

        return new BaseStrategy(entryRule, exitRule);
    }

}
