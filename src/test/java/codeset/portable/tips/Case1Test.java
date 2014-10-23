package codeset.portable.tips;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

public class Case1Test {

    private static HazelcastInstance instance1;
    private static HazelcastInstance instance2;

    @Before
    public void before() {
        Config config = new Config();
        config.getSerializationConfig().addPortableFactory(1, new MyPortableFactory());
        instance1 = Hazelcast.newHazelcastInstance(config);
        instance2 = Hazelcast.newHazelcastInstance(config);
    }

    @After
    public void after() {
        instance1.shutdown();
        instance2.shutdown();
    }

    @Test
    public void testAllPropertiesPopulated() {
        String key = UUID.randomUUID().toString();
        PortableClass portableClass = new PortableClass();
        portableClass.setBooleanProperty(true);
        portableClass.setDateProperty(new Date());
        portableClass.setDoubleProperty(123.456);
        portableClass.setLongProperty(123456789L);
        portableClass.setIntProperty(1234);
        portableClass.setStringProperty("string property");
        portableClass.setNestedProperty(new NestedPortableClass());
        List<NestedPortableClass> listProperty = new ArrayList<>();
        listProperty.add(new NestedPortableClass());
        portableClass.setListProperty(listProperty);

        IMap<String, PortableClass> writeMap = instance1.getMap("PortableClassMap");
        writeMap.set(key, portableClass);

        IMap<String, PortableClass> readMap = instance2.getMap("PortableClassMap");
        PortableClass result = readMap.get(key);
        assertNotNull(result);
    }

    @Test
    public void testNoPropertiesPopulated() {
        String key = UUID.randomUUID().toString();
        PortableClass portableClass = new PortableClass();

        IMap<String, PortableClass> writeMap = instance1.getMap("PortableClassMap");
        writeMap.set(key, portableClass);

        IMap<String, PortableClass> readMap = instance2.getMap("PortableClassMap");
        try {
            PortableClass result = readMap.get(key);
            fail("Should fail because of unknown fields");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
