package org.mitre.caasd.commons.func;

import static java.lang.Math.sqrt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class TranslatingConsumerTest {


    @Test
    public void basicUsage() {

        //Bundle a "String to Double" function and a downstream Consumer<Double>.
        //Allows the "combined item" to be used as a Consumer<String>.
        TranslatingConsumer<String, Double> stringConsumer = new TranslatingConsumer<>(
            str -> sqrt(str.length()),
            new AggregatingConsumer()
        );

        stringConsumer.accept("a"); //sqrt(1)
        stringConsumer.accept("bb");  //sqrt(2)
        stringConsumer.accept("ccc");  //sqrt(3)
        stringConsumer.accept("dddd");  //sqrt(4)

        double actualSum = ((AggregatingConsumer) stringConsumer.consumer()).sum;
        double actualCount = ((AggregatingConsumer) stringConsumer.consumer()).count;

        assertThat(actualSum, closeTo(sqrt(1) + sqrt(2) + sqrt(3) + sqrt(4), 0.001));
        assertThat(actualCount, is(4.0));
    }


    static class AggregatingConsumer implements Consumer<Double> {

        double sum = 0;

        int count = 0;

        @Override
        public void accept(Double aDouble) {
            sum += aDouble;
            count++;
        }
    }

}