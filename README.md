portable-tricks
===============

The aim of this article is to give you some tips on how to use the Portable format. I will demonstrate a simple persistence approach and show how you can get around some of the challenges.

We’ll start with a couple of domain objects which we would like to save in a Hazelcast map. The PortableClass has a number of simple properties and a nested object property of type NestedPortableClass. They both implement the com.hazelcast.nio.serialization.Portable interface.

Saving and retrieving these classes should cover most of the common serialization scenarios.
```
public class PortableClass implements Portable {
    private Date dateProperty;
    private Integer intProperty;
    private Long longProperty;
    private Double doubleProperty;
    private String stringProperty;
    private Boolean booleanProperty;
    private NestedPortableClass nestedProperty;
    private List<NestedPortableClass> listProperty = new ArrayList<>();
```
The NestedPropertyClass is pretty much the same as the PortableClass but with no nested Portable properties.
```
    private NestedPortableClass nestedProperty;
```
The readPortable method is used for reading the byte stream from the wire and assigning the data to the object’s properties.
```
    @Override
    public void readPortable(PortableReader reader) throws IOException {
        Long datePropertyAsLong = reader.readLong("dateProperty");
        if(datePropertyAsLong != null) {
            dateProperty = new Date(datePropertyAsLong);
        }
        intProperty = reader.readInt("intProperty");
        longProperty = reader.readLong("longProperty");
        doubleProperty = reader.readDouble("doubleProperty");
        stringProperty = reader.readUTF("stringProperty");
        booleanProperty = reader.readBoolean("booleanProperty");
        nestedProperty = reader.readPortable("nestedProperty");
        Portable[] listPropertyArr = reader.readPortableArray("listProperty");
        for (Portable p:listPropertyArr) {
            listProperty.add((NestedPortableClass) p);  
        }
    }
```
The writePortable method is used for writing data to the wire based on the object’s value.
```
    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        if(dateProperty != null) {
            writer.writeLong("dateProperty", dateProperty.getTime());
        }
        if(intProperty != null) {
            writer.writeInt("intProperty", intProperty);
        }
        if(longProperty != null) {
            writer.writeLong("longProperty", longProperty);
        }
        if(doubleProperty != null) {
            writer.writeDouble("doubleProperty", doubleProperty);
        }
        if(stringProperty != null) {
            writer.writeUTF("stringProperty", stringProperty);
        }
        if(booleanProperty != null) {
            writer.writeBoolean("booleanProperty", booleanProperty);
        }
        if(nestedProperty != null) {
            writer.writePortable("nestedProperty", nestedProperty);
        }
        if(listProperty != null && !listProperty.isEmpty()) {
            writer.writePortableArray("listProperty", listProperty.toArray(new Portable[listProperty.size()]));
        }
    }
```
The factory id is used by Hazelcast to determine which PortableFactory to use. You need to register a PortableFactory implementation to the serialization config. More about that later.
```
    @Override
    public int getFactoryId() {
        return 1;
    }
```
The class id is used by the PortableFactory to determine which class it should instantiate. Of course this could be done with reflection, but it would have a performance impact.
```
    @Override
    public int getClassId() {
        return 1;
    }
```
To be able to use these classes we need to create a PortableFactory implementation. It will be used by Hazelcast when an object is being serialized to create new instances.
```
public class MyPortableFactory implements PortableFactory {
    @Override
    public Portable create(int classId) {
        if(classId == 1) {
            return new PortableClass();
        } else if(classId == 2) {
            return new NestedPortableClass();
        } else {
            throw new IllegalArgumentException(classId + " unsupported classId");
        }
    }
}
```
Next we’ll create a test case to show some of the challenges we need to address. The first test case will attempt to serialize a PortableClass with no properties populated.

All tests follow the same setup as per below. 
```
public class Test {

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
```
####Case 1 - All properties populated
When all properties are populated with values everything works perfectly fine.
```
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
```
####Case 2 - No properties populated
Next we’ll try serialization with no properties populated. An error is expected caused by unknown fields. The reason is that by default, Hazelcast will create its internal definition of a class based on the first instance serialized. Because the properties are null, Hazelcast will not add them to the definition.

Stack trace:
```
com.hazelcast.nio.serialization.HazelcastSerializationException: Unknown field name: 'dateProperty' for ClassDefinition {id: 1, version: 0}
	at com.hazelcast.nio.serialization.DefaultPortableReader.throwUnknownFieldException(DefaultPortableReader.java:222)
	at com.hazelcast.nio.serialization.DefaultPortableReader.readNestedPosition(DefaultPortableReader.java:301)
	at com.hazelcast.nio.serialization.DefaultPortableReader.readPosition(DefaultPortableReader.java:261)
	at com.hazelcast.nio.serialization.DefaultPortableReader.readLong(DefaultPortableReader.java:77)
	at codeset.portable.tips.PortableClass.readPortable(PortableClass.java:31)
```

```
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
            
        }
    }
```
####Case 3 - Class definitions provided
To make it work we need to manually define and add class definitions for all classes. We use the com.hazelcast.nio.serialization.ClassDefinitionBuilder to create these. The constructor arguments to the builder is the FactoryId and the ClassId for the class.
```
    @Before
    public void before() {
        Config config = new Config();
        config.getSerializationConfig().addPortableFactory(1, new MyPortableFactory());

        ClassDefinitionBuilder nestedPortableClassBuilder = new ClassDefinitionBuilder(1, 1);
        nestedPortableClassBuilder.addLongField("dateProperty");
        nestedPortableClassBuilder.addIntField("intProperty");
        nestedPortableClassBuilder.addLongField("longProperty");
        nestedPortableClassBuilder.addDoubleField("doubleProperty");
        nestedPortableClassBuilder.addUTFField("stringProperty");
        nestedPortableClassBuilder.addBooleanField("booleanProperty");
        ClassDefinition nestedPortableClassDefinition = nestedPortableClassBuilder.build();
        config.getSerializationConfig().addClassDefinition(nestedPortableClassDefinition);

        ClassDefinitionBuilder portableClassBuilder = new ClassDefinitionBuilder(1, 2);
        portableClassBuilder.addLongField("dateProperty");
        portableClassBuilder.addIntField("intProperty");
        portableClassBuilder.addLongField("longProperty");
        portableClassBuilder.addDoubleField("doubleProperty");
        portableClassBuilder.addUTFField("stringProperty");
        portableClassBuilder.addBooleanField("booleanProperty");
        portableClassBuilder.addPortableField("nestedProperty", nestedPortableClassDefinition);
        portableClassBuilder.addPortableArrayField("listProperty", nestedPortableClassDefinition);
        config.getSerializationConfig().addClassDefinition(portableClassBuilder.build());

        instance1 = Hazelcast.newHazelcastInstance(config);
        instance2 = Hazelcast.newHazelcastInstance(config);
    }
```
If the test case is executed again, the unknown field error should have gone, but instead you’re likely to see something like:
```
Caused by: java.io.UTFDataFormatException: Length check failed, maybe broken bytestream or wrong stream position
	at com.hazelcast.nio.UTFEncoderDecoder.readUTF0(UTFEncoderDecoder.java:506)
	at com.hazelcast.nio.UTFEncoderDecoder.readUTF(UTFEncoderDecoder.java:78)
	at com.hazelcast.nio.serialization.ByteArrayObjectDataInput.readUTF(ByteArrayObjectDataInput.java:450)
	at com.hazelcast.nio.serialization.DefaultPortableReader.readUTF(DefaultPortableReader.java:86)
	at codeset.portable.tips.PortableClass.readPortable(PortableClass.java:38)
```
Hazelcast is still not happy, null UTF fields are still a problem. Adding a check to see if the field exists doesn’t help:
```
        if(reader.hasField("stringProperty")) {
            stringProperty = reader.readUTF("stringProperty");
        }
```
####Case 4 - Null checks added
The only way we got around this was by adding a null check flag to the byte stream. Note: this also has to be added to the class definition.

In readPortable() add the following:
```
        if(reader.readBoolean("__hasValue_stringProperty")) {
            stringProperty = reader.readUTF("stringProperty");
        }
```
In writePortable() add the following:
```
        if(stringProperty != null) {
            writer.writeUTF("stringProperty", stringProperty);
            writer.writeBoolean("__hasValue_stringProperty", true);
        }
```
In your class definition building add:
```
        ClassDefinitionBuilder portableClassBuilder = new ClassDefinitionBuilder(1, 1);
	....
        portableClassBuilder.addUTFField("stringProperty");
        portableClassBuilder.addBooleanField("__hasValue_stringProperty");
	....
```
Running the test again with completely empty objects should work just fine. But no, this will most likely happen:

Stack trace:
```
com.hazelcast.nio.serialization.HazelcastSerializationException: java.lang.IllegalArgumentException
	....
Caused by: java.lang.IllegalArgumentException
	at com.hazelcast.nio.serialization.ByteArrayObjectDataInput.position(ByteArrayObjectDataInput.java:487)
	at com.hazelcast.nio.serialization.DefaultPortableReader.end(DefaultPortableReader.java:318)
	at com.hazelcast.nio.serialization.PortableSerializer.read(PortableSerializer.java:80)
	at com.hazelcast.nio.serialization.PortableSerializer.readAndInitialize(PortableSerializer.java:108)
	at com.hazelcast.nio.serialization.DefaultPortableReader.readPortable(DefaultPortableReader.java:213)
	at codeset.portable.tips.PortableClass.readPortable(PortableClass.java:42)
	at com.hazelcast.nio.serialization.PortableSerializer.read(PortableSerializer.java:79)
	at com.hazelcast.nio.serialization.PortableSerializer.read(PortableSerializer.java:66)
	at com.hazelcast.nio.serialization.PortableSerializer.read(PortableSerializer.java:29)
	at com.hazelcast.nio.serialization.StreamSerializerAdapter.read(StreamSerializerAdapter.java:63)
	at com.hazelcast.nio.serialization.SerializationServiceImpl.readObject(SerializationServiceImpl.java:285)
	at com.hazelcast.nio.serialization.SerializationServiceImpl.toObject(SerializationServiceImpl.java:262)
	... 31 more
```
We need to add the null checks to the nested complex properties as well:

In readPortable() add the following:
```
        if(reader.readBoolean("__hasValue_nestedProperty")) {
            nestedProperty = reader.readPortable("nestedProperty");
        }
        if(reader.readBoolean("__hasValue_listProperty")) {
            Portable[] listPropertyArr = reader.readPortableArray("listProperty");
            for (Portable p:listPropertyArr) {
                listProperty.add((NestedPortableClass) p);  
            }
        }
```
In writePortable() add the following:
```
        if(nestedProperty != null) {
            writer.writePortable("nestedProperty", nestedProperty);
            writer.writeBoolean("__hasValue_nestedProperty", true);
        }
        if(listProperty != null && !listProperty.isEmpty()) {
            writer.writePortableArray("listProperty", listProperty.toArray(new Portable[listProperty.size()]));
            writer.writeBoolean("__hasValue_nestedProperty", true);
        }
```
In your class definition building add:
```
        ClassDefinitionBuilder portableClassBuilder = new ClassDefinitionBuilder(1, 1);
	....
        portableClassBuilder.addPortableField("nestedProperty", nestedPortableClassDefinition);
        portableClassBuilder.addBooleanField("__hasValue_nestedProperty");
        portableClassBuilder.addPortableArrayField("listProperty", nestedPortableClassDefinition);
        portableClassBuilder.addBooleanField("__hasValue_listProperty");
	....
```

Now it works!

codeset provides a reflection based ClassDefinitionBuilder [INSERT REFERENCE]. It will reflect on a class and build the class definition during configuration including the above logic. 
