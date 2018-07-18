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
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class XChangeTest {

    private static CurrencyPair pair = CurrencyPair.BTC_USDT;
    private static KlineInterval interval = KlineInterval.m15;
    private static int limit = 500;


    public static void main(String[] args) throws IOException, InterruptedException {
        ExchangeSpecification binanceSpecification = new BinanceExchange().getDefaultExchangeSpecification();
        binanceSpecification.setApiKey("");
        binanceSpecification.setSecretKey("");
        Exchange binance = ExchangeFactory.INSTANCE.createExchange(binanceSpecification);
////        Exchange bittrex = ExchangeFactory.INSTANCE.createExchange(BittrexExchange.class.getName());
////        MarketDataService dataServiceBinance = binance.getMarketDataService();
////        MarketDataService dataServiceBittrex = bittrex.getMarketDataService();
//        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
//        loggerContext.stop();
//
        String exchangeName = binance.getExchangeSpecification().getExchangeName();
//        BinanceMarketDataService marketDataService = (BinanceMarketDataService) binance.getMarketDataService();
//        List<BinanceKline> klines = getBinanceKlines(marketDataService, 360L);
//        writeCsv(klines, "btc_usdt_15m_bars.csv");
//        TimeSeries series = new BaseTimeSeries.SeriesBuilder().withName(exchangeName + "TS").withNumTypeOf(BigDecimalNum::valueOf).build();
//        for (BinanceKline kline : klines) {
//            ZonedDateTime dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(kline.getCloseTime()), ZoneId.systemDefault());
//            series.addBar(dateTime, BigDecimalNum.valueOf(kline.getOpenPrice()),
//                    BigDecimalNum.valueOf(kline.getHighPrice()), BigDecimalNum.valueOf(kline.getLowPrice()),
//                    BigDecimalNum.valueOf(kline.getClosePrice()), BigDecimalNum.valueOf(kline.getVolume()));
//        }


        TimeSeries series = CsvBarsLoader.loadBtcUsdtSeries();
        Strategy strategy = Strategies.getRsiEmaStrategy(series, 40, 60, 7, 7, 28);

//        TimeSeriesCollection dataSet = new TimeSeriesCollection();
//        String title = exchangeName + " " + pair.toString();
//        dataSet.addSeries(buildChartTimeSeries(series, new ClosePriceIndicator(series), title));
//        createChart(dataSet, series, strategy, title);

        TimeSeriesManager seriesManager = new TimeSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        analytics(tradingRecord, series);
    }

    private static void analytics(TradingRecord tradingRecord, TimeSeries series) {
        System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());
        System.out.println("Total profit for the strategy: " + new TotalProfitCriterion().calculate(series, tradingRecord));
        List<Trade> trades = tradingRecord.getTrades();
        int profitCount = 0;
        int lossCount = 0;
        Num sum = BigDecimalNum.valueOf("15");
        for (Trade trade : trades){
            Num priceEntry = trade.getEntry().getPrice();
            Num priceExit = trade.getExit().getPrice();
            if (priceExit.isGreaterThan(priceEntry)) {
                profitCount++;
//                sum = sum.minus(sum.multipliedBy(BigDecimalNum.valueOf("0.001")));
                sum = sum.plus(sum.multipliedBy(BigDecimalNum.valueOf("0.009")));
            } else {
                lossCount++;
//                sum = sum.minus(sum.multipliedBy(BigDecimalNum.valueOf("0.001")));
                sum = sum.minus(sum.multipliedBy(BigDecimalNum.valueOf("0.021")));
            }
        }
        System.out.println("Profit trade: " + profitCount + " loss trade: " + lossCount + " ratio: " + (double)lossCount/profitCount);
        System.out.println(sum.toString());
    }

    private static void writeCsv(List<BinanceKline> klines, String fileName) {
        String line;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try(FileWriter writer = new FileWriter(fileName, true))
        {
            for (BinanceKline kline : klines){
                line = dateFormat.format(new Date(kline.getCloseTime())) + "," + kline.getOpenPrice() + "," + kline.getHighPrice() + "," + kline.getLowPrice()  + "," + kline.getClosePrice()  + "," + kline.getVolume() + "\r\n";
                writer.write(line);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<BinanceKline> getBinanceKlines(BinanceMarketDataService dataService, long days) throws IOException {
        long daysInMs = days * 24 * 60 * 60 * 1000;
        long fiveDaysInMs = 432000000;
        long startDate = System.currentTimeMillis() - daysInMs;
        long endDate = startDate + fiveDaysInMs;
        List<BinanceKline> klines = new ArrayList<>();
        int countIteration = (int) Math.ceil(daysInMs / fiveDaysInMs);
        for (int i = 0; i < countIteration; i++) {
            klines.addAll(dataService.klines(pair, interval, limit, startDate, endDate));
            startDate = endDate;
            endDate = startDate + fiveDaysInMs;
            if (new Date(endDate).after(new Date(System.currentTimeMillis()))) {
                endDate = System.currentTimeMillis();
            }
        }
        return klines;
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
        TradingRecord tradingRecord = seriesManager.run(strategy);
        analytics(tradingRecord, series);

        // Adding markers to plot
        for (Trade trade : tradingRecord.getTrades()) {
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
