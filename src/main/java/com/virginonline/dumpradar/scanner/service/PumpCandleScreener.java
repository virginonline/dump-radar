package com.virginonline.dumpradar.scanner.service;

import com.virginonline.dumpradar.config.ScreenProperties;
import com.virginonline.dumpradar.scanner.model.Candle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;


@Service
public class PumpCandleScreener {
    private final ScreenProperties props;

    public PumpCandleScreener(ScreenProperties props) {
        this.props = props;
    }

    public Optional<Candle> screen(List<Candle> candles) {
        if (candles.size() < props.historyBars() + 1) return Optional.empty();
        Candle signal = candles.getLast();

        if (!bodyIsStrongEnough(signal)) return Optional.empty();
        if (!volumeSpikes(candles, signal)) return Optional.empty();
        if (!closesNearHigh(signal)) return Optional.empty();

        return Optional.of(signal);
    }

    private boolean bodyIsStrongEnough(Candle candle) {
        return candle.close()
                .subtract(candle.open())
                .divide(candle.open(), 10, RoundingMode.HALF_UP)
                .compareTo(props.minBodyPct()) >= 0;
    }

    private boolean volumeSpikes(List<Candle> candles, Candle signal) {
        List<Candle> history = candles.subList(candles.size() - 1 - props.historyBars(), candles.size() - 1);

        BigDecimal sum = BigDecimal.ZERO;
        for (Candle c : history) {
            sum = sum.add(c.volumeQuote());
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(props.historyBars()), 10, RoundingMode.HALF_UP);

        return signal.volumeQuote().compareTo(props.volumeMultiple().multiply(avg)) >= 0;
    }

    private boolean closesNearHigh(Candle candle) {
        return candle.high().subtract(candle.close())
                .compareTo(props.maxCloseFromHigh().multiply(candle.high().subtract(candle.low()))) <= 0;
    }

}
