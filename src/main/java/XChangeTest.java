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
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Binance BTC/USDT ticker")
                .xAxisTitle("BTC")
                .yAxisTitle("USD")
                .build();
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        List<Date> xData = new ArrayList<>();
        List<Number> yData = new ArrayList<>();
        BinanceMarketDataService marketDataService = (BinanceMarketDataService) binance.getMarketDataService();
        List<BinanceKline> klines = marketDataService.klines(CurrencyPair.BTC_USDT, KlineInterval.m15, 100, null, null);
        for (BinanceKline kline : klines) {
            xData.add(new Date(kline.getCloseTime()));
            yData.add(kline.getClosePrice());
        }
        Collections.reverse(xData);
        Collections.reverse(yData);
        XYSeries series = chart.addSeries("bids", xData, yData);
        series.setMarker(SeriesMarkers.CIRCLE);
        new SwingWrapper(chart).displayChart();


//        while (true) {
//            BigDecimal lastBinance = dataServiceBinance.getTicker(CurrencyPair.BTC_USDT).getLast();
//            BigDecimal lastBittrex = dataServiceBittrex.getTicker(CurrencyPair.BTC_USDT).getLast();
//            System.out.println(lastBinance + " " + lastBittrex);
//            System.out.println("Разница между ценами BTCUSDT на Binance и Bittrex - " +
//                    Math.abs(lastBinance.subtract(lastBittrex).doubleValue()) + " USDT");
//            Thread.sleep(5000);
//        }
    }
}
