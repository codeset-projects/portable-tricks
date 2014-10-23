package codeset.portable.tips;

import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.nio.serialization.ClassDefinition;
import com.hazelcast.nio.serialization.ClassDefinitionBuilder;

public class Case2Test {

    private static HazelcastInstance instance1;
    private static HazelcastInstance instance2;

    @Before
    public void before() {
        Config config = new Config();
        config.getSerializationConfig().addPortableFactory(1, new MyPortableFactory());

        ClassDefinitionBuilder nestedPortableClassBuilder = new ClassDefinitionBuilder(1, 2);
        nestedPortableClassBuilder.addLongField("dateProperty");
        nestedPortableClassBuilder.addIntField("intProperty");
        nestedPortableClassBuilder.addLongField("longProperty");
        nestedPortableClassBuilder.addDoubleField("doubleProperty");
        nestedPortableClassBuilder.addUTFField("stringProperty");
        nestedPortableClassBuilder.addBooleanField("__hasValue_stringProperty");
        nestedPortableClassBuilder.addBooleanField("booleanProperty");
        ClassDefinition nestedPortableClassDefinition = nestedPortableClassBuilder.build();
        config.getSerializationConfig().addClassDefinition(nestedPortableClassDefinition);

        ClassDefinitionBuilder portableClassBuilder = new ClassDefinitionBuilder(1, 1);
        portableClassBuilder.addLongField("dateProperty");
        portableClassBuilder.addIntField("intProperty");
        portableClassBuilder.addLongField("longProperty");
        portableClassBuilder.addDoubleField("doubleProperty");
        portableClassBuilder.addUTFField("stringProperty");
        portableClassBuilder.addBooleanField("__hasValue_stringProperty");
        portableClassBuilder.addBooleanField("booleanProperty");
        portableClassBuilder.addPortableField("nestedProperty", nestedPortableClassDefinition);
        portableClassBuilder.addBooleanField("__hasValue_nestedProperty");
        portableClassBuilder.addPortableArrayField("listProperty", nestedPortableClassDefinition);
        portableClassBuilder.addBooleanField("__hasValue_listProperty");
        config.getSerializationConfig().addClassDefinition(portableClassBuilder.build());

        instance1 = Hazelcast.newHazelcastInstance(config);
        instance2 = Hazelcast.newHazelcastInstance(config);
    }

    @After
    public void after() {
        instance1.shutdown();
        instance2.shutdown();
    }

    @Test
    public void testNoPropertiesPopulated() {
        String key = UUID.randomUUID().toString();
        PortableClass portableClass = new PortableClass();

        IMap<String, PortableClass> writeMap = instance1.getMap("PortableClassMap");
        writeMap.set(key, portableClass);

        IMap<String, PortableClass> readMap = instance2.getMap("PortableClassMap");
        PortableClass result = readMap.get(key);
        assertNotNull(result);
    }

}
