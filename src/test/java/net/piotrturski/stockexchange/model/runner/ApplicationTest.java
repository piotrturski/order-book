package net.piotrturski.stockexchange.model.runner;

import com.diffplug.common.base.Errors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.piotrturski.stockexchange.model.LimitOrder;
import net.piotrturski.stockexchange.runner.Application;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.util.Scanner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationTest {

    @Test
    public void should_work_with_input_output_streams() throws Exception {

        PipedOutputStream testInput = new PipedOutputStream();
        PipedInputStream testOutput = new PipedInputStream(1000);

        PipedInputStream input = new PipedInputStream(testInput, 1000);
        PipedOutputStream output = new PipedOutputStream(testOutput);

        Thread thread = new Thread(Errors.rethrow().wrap(() -> new Application().run(input,output)));
        thread.start();

        Scanner sc = new Scanner(testOutput);
        sc.useDelimiter("\n");

        // copy-paste from pdf sample
        MutableInt position = new MutableInt(0);

        EntryStream.of(
                "{\"type\":\"Limit\",\"order\":{\"direction\":\"Buy\",\"id\":1,\"price\":14,\"quantity\":20}}",
                "{\"buyOrders\":[{\"id\":1,\"price\":14,\"quantity\":20}],\"sellOrders\":[]}",

                "{\"type\":\"Iceberg\",\"order\":{\"direction\":\"Buy\",\"id\":2,\"price\":15,\"quantity\":50,\"peak\":20}}",
                "{\"buyOrders\": [{\"id\": 2, \"price\": 15, \"quantity\": 20}, {\"id\": 1, \"price\": 14, \"quantity\": 20}],\"sellOrders\":[]}",

                "{\"type\":\"Limit\",\"order\":{\"direction\":\"Sell\",\"id\":3,\"price\":16,\"quantity\":15}}",
                "{\"buyOrders\": [{\"id\": 2, \"price\": 15, \"quantity\": 20}, {\"id\": 1, \"price\": 14, \"quantity\": 20}],\"sellOrders\":[{\"id\":3,\"price\":16,\"quantity\":15}]}",

                "{\"type\":\"Limit\",\"order\":{\"direction\":\"Sell\",\"id\":4,\"price\":13,\"quantity\":60}}",
                "{\"buyOrders\": [{\"id\": 1, \"price\": 14, \"quantity\": 10}], \"sellOrders\": [{\"id\": 3, \"price\": 16,\"quantity\":15}]}\n" +
                        "{\"buyOrderId\":2,\"sellOrderId\":4,\"price\":15,\"quantity\":20}\n" +
                        "{\"buyOrderId\":2,\"sellOrderId\":4,\"price\":15,\"quantity\":20}\n" +
                        "{\"buyOrderId\":2,\"sellOrderId\":4,\"price\":15,\"quantity\":10}\n" +
                        "{\"buyOrderId\":1,\"sellOrderId\":4,\"price\":14,\"quantity\":10}"
        )
                .forKeyValue((k, v) -> Errors.rethrow().run(() -> {
                    int pos = position.incrementAndGet();
                    IOUtils.write(k + '\n', testInput, UTF_8);

                    StreamEx.split(v, '\n').forEach(Errors.rethrow().wrap(line -> {
                        JSONAssert.assertEquals("at position " + pos, line, sc.next(), true);
                    }));
                }));

        testInput.close();

        thread.join();
    }

    //    {"type":"Limit","order":{"direction":"Buy","id":1,"price":14,"quantity":20}}
//    {"sellOrders":[],"buyOrders":[{"id":1,"price":14,"quantity":20}]}
//    {"type":"Iceberg","order":{"direction":"Buy","id":2,"price":15,"quantity":50,"peak":20}}
//    {"sellOrders":[],"buyOrders":[{"id":2,"price":15,"quantity":50},{"id":1,"price":14,"quantity":20}]}
//
//
//    {"type":"Limit","order":{"direction":"Sell","id":3,"price":16,"quantity":15}}
//    {"sellOrders":[{"id":3,"price":16,"quantity":15}],"buyOrders":[{"id":2,"price":15,"quantity":50},{"id":1,"price":14,"quantity":20}]}
//    {"type":"Limit","order":{"direction":"Sell","id":4,"price":13,"quantity":60}}
//    {"sellOrders":[{"id":4,"price":13,"quantity":40},{"id":3,"price":16,"quantity":15}],"buyOrders":[]}
//    {"buyOrderId":2,"sellOrderId":4,"quantity":50,"price":15}
//    {"buyOrderId":2,"sellOrderId":4,"quantity":-50,"price":15}
//    {"buyOrderId":1,"sellOrderId":4,"quantity":20,"price":14}


    @Test
    public void should_deserialize_using_custom_constructor() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new ParameterNamesModule());

        LimitOrder limitOrder = objectMapper.readValue(
                "{\"direction\":\"Buy\",\"id\":1,\"price\":14,\"quantity\":20}", LimitOrder.class);
    }

    @Test
    public void parameters_name_reflection_should_be_active() throws Exception {
        Method main = StreamEx.of(Application.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("main"))
                .findFirst()
                .get();

        String name = main.getParameters()[0].getName();

        assertThat(name).isEqualTo("args");
    }
}

