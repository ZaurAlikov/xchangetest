import ch.qos.logback.classic.LoggerContext;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.LoggerFactory;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.PreviousValueIndicator;
import org.ta4j.core.num.BigDecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.trading.rules.*;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;

public class XChangeTest {

    private static CurrencyPair pair = CurrencyPair.LTC_USDT;
    private static KlineInterval interval = KlineInterval.m15;
    private static int limit = 500;


    public static void main(String[] args) throws IOException, InterruptedException {
        ExchangeSpecification binanceSpecification = new BinanceExchange().getDefaultExchangeSpecification();
        binanceSpecification.setApiKey("");
        binanceSpecification.setSecretKey("");
        Exchange binance = ExchangeFactory.INSTANCE.createExchange(binanceSpecification);
//        Exchange bittrex = ExchangeFactory.INSTANCE.createExchange(BittrexExchange.class.getName());
//        MarketDataService dataServiceBinance = binance.getMarketDataService();
//        MarketDataService dataServiceBittrex = bittrex.getMarketDataService();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();

        String exchangeName = binance.getExchangeSpecification().getExchangeName();
        BinanceMarketDataService marketDataService = (BinanceMarketDataService) binance.getMarketDataService();
        List<BinanceKline> klines = getBinanceKlines(marketDataService, 30L);
        TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName(exchangeName + "TS").withNumTypeOf(BigDecimalNum::valueOf).build();
        for (BinanceKline kline : klines) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());
            series.addBar(dateTime, BigDecimalNum.valueOf(kline.getOpenPrice()),
                    BigDecimalNum.valueOf(kline.getHighPrice()), BigDecimalNum.valueOf(kline.getLowPrice()),
                    BigDecimalNum.valueOf(kline.getClosePrice()), BigDecimalNum.valueOf(kline.getVolume()));
        }

        Strategy strategy = buildStrategy(series);
//        TimeSeriesCollection dataSet = new TimeSeriesCollection();
//        String title = exchangeName + " " + pair.toString();
//        dataSet.addSeries(buildChartTimeSeries(series, new ClosePriceIndicator(series), title));
//        createChart(dataSet, series, strategy, title);


        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));

//        while (true) {
//            BigDecimal lastBinance = dataServiceBinance.getTicker(CurrencyPair.NEO_USDT).getLast();
//            BigDecimal lastBittrex = dataServiceBittrex.getTicker(CurrencyPair.NEO_USDT).getLast();
//            System.out.println(lastBinance + " " + lastBittrex);
//            System.out.println("Разница между ценами BTCUSDT на Binance и Bittrex - " +
//                    Math.abs(lastBinance.subtract(lastBittrex).doubleValue()) + " USDT");
//            Thread.sleep(1000);
//        }
    }

    private static List<BinanceKline> getBinanceKlines(BinanceMarketDataService dataService, long days) throws IOException {
        long daysInMs = days * 24 * 60 * 60 * 1000;
        long fiveDaysInMs = 432000000;
        long startDate = System.currentTimeMillis() - daysInMs;
        long endDate = startDate + fiveDaysInMs;
        List<BinanceKline> klines = new ArrayList<>();
        int countIteration = (int) Math.ceil(daysInMs / fiveDaysInMs);
        for (int i = 0; i < countIteration; i++){
            klines.addAll(dataService.klines(pair, interval, limit, startDate, endDate));
            startDate = endDate;
            endDate = startDate + fiveDaysInMs;
            if (new Date(endDate).after(new Date(System.currentTimeMillis()))) {
                endDate = System.currentTimeMillis();
            }
        }
        return klines;
    }

    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 7);
        EMAIndicator longEma = new EMAIndicator(closePrice, 28);
        RSIIndicator rsiIndicator = new RSIIndicator(closePrice, 7);
        PreviousValueIndicator rsiIndicator1 = new PreviousValueIndicator(rsiIndicator, 1);
        PreviousValueIndicator rsiIndicator2 = new PreviousValueIndicator(rsiIndicator, 2);
        PreviousValueIndicator rsiIndicator3 = new PreviousValueIndicator(rsiIndicator, 3);
        PreviousValueIndicator rsiIndicator4 = new PreviousValueIndicator(rsiIndicator, 4);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(shortEma, longEma)
                .and(new OverIndicatorRule(rsiIndicator, BigDecimalNum.valueOf(60)))
                .and(new UnderIndicatorRule(rsiIndicator1, BigDecimalNum.valueOf(40))
                        .or(new UnderIndicatorRule(rsiIndicator2, BigDecimalNum.valueOf(40))
                                .or(new UnderIndicatorRule(rsiIndicator3, BigDecimalNum.valueOf(40))
                                        .or(new UnderIndicatorRule(rsiIndicator4, BigDecimalNum.valueOf(40))))))
                .and(new UnderIndicatorRule(rsiIndicator, rsiIndicator1)
                        .and(new UnderIndicatorRule(rsiIndicator, rsiIndicator2)
                                .and(new UnderIndicatorRule(rsiIndicator1, rsiIndicator2)
                                        .and(new UnderIndicatorRule(rsiIndicator2, rsiIndicator3)))));
        // Exit rule
        Rule exitRule = new StopLossRule(closePrice, BigDecimalNum.valueOf("2"))
                .or(new StopGainRule(closePrice, BigDecimalNum.valueOf("1")));

        return new BaseStrategy(entryRule, exitRule);
    }

    private static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries barseries, Indicator<Num> indicator, String name) {
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < barseries.getBarCount(); i++) {
            Bar bar = barseries.getBar(i);
            chartTimeSeries.add(new Minute(Date.from(bar.getEndTime().toInstant())), indicator.getValue(i).doubleValue());
        }
        return chartTimeSeries;
    }

    private static void addBuySellSignals(TimeSeries series, Strategy strategy, XYPlot plot) {
        // Running the strategy
        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        List<Trade> trades = seriesManager.run(strategy).getTrades();
        // Adding markers to plot
        for (Trade trade : trades) {
            // Buy signal
            double buySignalBarTime = new Minute(Date.from(series.getBar(trade.getEntry().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker buyMarker = new ValueMarker(buySignalBarTime);
            buyMarker.setPaint(Color.GREEN);
            buyMarker.setLabel("B");
            plot.addDomainMarker(buyMarker);
            // Sell signal
            double sellSignalBarTime = new Minute(Date.from(series.getBar(trade.getExit().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker sellMarker = new ValueMarker(sellSignalBarTime);
            sellMarker.setPaint(Color.RED);
            sellMarker.setLabel("S");
            plot.addDomainMarker(sellMarker);
        }
    }

    private static void createChart(TimeSeriesCollection dataset, TimeSeries series, Strategy strategy, String title) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title, // title
                "Date", // x-axis label
                "Price", // y-axis label
                dataset, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        );
        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));
        addBuySellSignals(series, strategy, plot);
        displayChart(chart);
    }

    private static void displayChart(JFreeChart chart) {
        // Chart panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(1024, 400));
        // Application frame
        ApplicationFrame frame = new ApplicationFrame("Buy and sell signals to chart");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }
}
