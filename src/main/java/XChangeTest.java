import ch.qos.logback.classic.LoggerContext;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.bittrex.BittrexExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.GainIndicator;
import org.ta4j.core.num.BigDecimalNum;
import org.ta4j.core.trading.rules.BooleanRule;
import org.ta4j.core.trading.rules.CrossedUpIndicatorRule;
import org.ta4j.core.trading.rules.UnderIndicatorRule;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class XChangeTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        ExchangeSpecification binanceSpecification = new BinanceExchange().getDefaultExchangeSpecification();
        binanceSpecification.setApiKey("");
        binanceSpecification.setSecretKey("");
        Exchange binance = ExchangeFactory.INSTANCE.createExchange(binanceSpecification);

        Exchange bittrex = ExchangeFactory.INSTANCE.createExchange(BittrexExchange.class.getName());
        MarketDataService dataServiceBinance = binance.getMarketDataService();
        MarketDataService dataServiceBittrex = bittrex.getMarketDataService();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();


        BinanceMarketDataService marketDataService = (BinanceMarketDataService) binance.getMarketDataService();
        List<BinanceKline> klines = marketDataService.klines(CurrencyPair.BTC_USDT, KlineInterval.m15, 500, null, null);
        TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName("binanceTS").withNumTypeOf(BigDecimalNum::valueOf).build();
        for (BinanceKline kline : klines) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());
            series.addBar(dateTime, BigDecimalNum.valueOf(kline.getOpenPrice()),
                    BigDecimalNum.valueOf(kline.getHighPrice()), BigDecimalNum.valueOf(kline.getLowPrice()),
                    BigDecimalNum.valueOf(kline.getClosePrice()), BigDecimalNum.valueOf(kline.getVolume()));
        }


        Strategy strategy = buildStrategy(series);
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));

//        XYChart chart = new XYChartBuilder()
//                .width(800)
//                .height(600)
//                .title("Binance BTC/USDT ticker")
//                .xAxisTitle("BTC")
//                .yAxisTitle("USD")
//                .build();
//        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
//        List<Date> xData = new ArrayList<>();
//        List<Number> yData = new ArrayList<>();
//        BinanceMarketDataService marketDataService = (BinanceMarketDataService) binance.getMarketDataService();
//        List<BinanceKline> klines = marketDataService.klines(CurrencyPair.BTC_USDT, KlineInterval.m15, 100, null, null);
//        for (BinanceKline kline : klines) {
//            xData.add(new Date(kline.getCloseTime()));
//            yData.add(kline.getClosePrice());
//        }
//        Collections.reverse(xData);
//        Collections.reverse(yData);
//        XYSeries series = chart.addSeries("bids", xData, yData);
//        series.setMarker(SeriesMarkers.CIRCLE);
//        new SwingWrapper(chart).displayChart();


//        while (true) {
//            BigDecimal lastBinance = dataServiceBinance.getTicker(CurrencyPair.NEO_USDT).getLast();
//            BigDecimal lastBittrex = dataServiceBittrex.getTicker(CurrencyPair.NEO_USDT).getLast();
//            System.out.println(lastBinance + " " + lastBittrex);
//            System.out.println("Разница между ценами BTCUSDT на Binance и Bittrex - " +
//                    Math.abs(lastBinance.subtract(lastBittrex).doubleValue()) + " USDT");
//            Thread.sleep(1000);
//        }
    }


    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        GainIndicator gainIndicator = new GainIndicator(closePrice);

        EMAIndicator shortEma = new EMAIndicator(closePrice, 7);
        EMAIndicator longEma = new EMAIndicator(closePrice, 28);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 7);


        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma)
                .and(new CrossedUpIndicatorRule(rsiIndicator, BigDecimalNum.valueOf(60)))
                .and(new BooleanRule(gainIndicator);


        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, new BigDecimal("80"))) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)); // Signal 2

        return new BaseStrategy(entryRule, exitRule);
    }
}
