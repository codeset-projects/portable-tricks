package codeset.portable.tips;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

/**
 * Sample domain object with a handful of different property types.
 * 
 * @author ingemar.svensson
 *
 */
public class PortableClass implements Portable {

    private Date dateProperty;
    private Integer intProperty;
    private Long longProperty;
    private Double doubleProperty;
    private String stringProperty;
    private Boolean booleanProperty;
    private NestedPortableClass nestedProperty;
    private List<NestedPortableClass> listProperty = new ArrayList<>();

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        Long datePropertyAsLong = reader.readLong("dateProperty");
        if(datePropertyAsLong != null) {
            dateProperty = new Date(datePropertyAsLong);
        }
        intProperty = reader.readInt("intProperty");
        longProperty = reader.readLong("longProperty");
        doubleProperty = reader.readDouble("doubleProperty");
        if(reader.readBoolean("__hasValue_stringProperty")) {
            stringProperty = reader.readUTF("stringProperty");
        }
        booleanProperty = reader.readBoolean("booleanProperty");
        if(reader.readBoolean("__hasValue_nestedProperty")) {
            nestedProperty = reader.readPortable("nestedProperty");
        }
        if(reader.readBoolean("__hasValue_listProperty")) {
            Portable[] listPropertyArr = reader.readPortableArray("listProperty");
            for (Portable p:listPropertyArr) {
                listProperty.add((NestedPortableClass) p);  
            }
        }
    }

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
            writer.writeBoolean("__hasValue_stringProperty", true);
        }
        if(booleanProperty != null) {
            writer.writeBoolean("booleanProperty", booleanProperty);
        }
        if(nestedProperty != null) {
            writer.writePortable("nestedProperty", nestedProperty);
            writer.writeBoolean("__hasValue_nestedProperty", true);
        }
        if(listProperty != null && !listProperty.isEmpty()) {
            writer.writePortableArray("listProperty", listProperty.toArray(new Portable[listProperty.size()]));
            writer.writeBoolean("__hasValue_nestedProperty", true);
        }
    }


    @Override
    public int getClassId() {
        return 1;
    }

    @Override
    public int getFactoryId() {
        return 1;
    }

    public Date getDateProperty() {
        return dateProperty;
    }

    public void setDateProperty(Date dateProperty) {
        this.dateProperty = dateProperty;
    }

    public Integer getIntProperty() {
        return intProperty;
    }

    public void setIntProperty(Integer intProperty) {
        this.intProperty = intProperty;
    }

    public Long getLongProperty() {
        return longProperty;
    }

    public void setLongProperty(Long longProperty) {
        this.longProperty = longProperty;
    }

    public Double getDoubleProperty() {
        return doubleProperty;
    }

    public void setDoubleProperty(Double doubleProperty) {
        this.doubleProperty = doubleProperty;
    }

    public String getStringProperty() {
        return stringProperty;
    }

    public void setStringProperty(String stringProperty) {
        this.stringProperty = stringProperty;
    }

    public Boolean getBooleanProperty() {
        return booleanProperty;
    }

    public void setBooleanProperty(Boolean booleanProperty) {
        this.booleanProperty = booleanProperty;
    }

    public NestedPortableClass getNestedProperty() {
        return nestedProperty;
    }

    public void setNestedProperty(NestedPortableClass nestedProperty) {
        this.nestedProperty = nestedProperty;
    }

    public List<NestedPortableClass> getListProperty() {
        return listProperty;
    }

    public void setListProperty(List<NestedPortableClass> listProperty) {
        this.listProperty = listProperty;
    }

}