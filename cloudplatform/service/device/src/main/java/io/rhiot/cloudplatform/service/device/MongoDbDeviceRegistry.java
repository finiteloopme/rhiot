package io.rhiot.cloudplatform.service.device;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.eclipse.cloudplatform.service.device.api.Device;

import java.util.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class MongoDbDeviceRegistry extends DisconnectionAwareDeviceRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper().
            configure(FAIL_ON_UNKNOWN_PROPERTIES, false).
            setSerializationInclusion(Include.NON_NULL);

    private final Mongo mongo;

    private final String db;

    private final String collection;

    // Constructors

    public MongoDbDeviceRegistry(Mongo mongo, String db, String collection, long disconnectionPeriod) {
        super(disconnectionPeriod);
        this.mongo = mongo;
        this.db = db;
        this.collection = collection;
    }

    // API operations

    @Override
    public Device get(String deviceId) {
        DBCursor devices = devicesCollection().find(new BasicDBObject(ImmutableMap.of("deviceId", deviceId)));
        if(devices.hasNext()) {
            return dbObjectToDevice(devices.next());
        }
        return null;
    }

    @Override
    public Device getByRegistrationId(String registrationId) {
        DBCursor devices = devicesCollection().find(new BasicDBObject(ImmutableMap.of("registrationId", registrationId)));
        if(devices.hasNext()) {
            return dbObjectToDevice(devices.next());
        }
        return null;    }

    public List<Device> list() {
        DBCursor devicesRecords = devicesCollection().find();
        List<Device> devices = new LinkedList<>();
        while (devicesRecords.hasNext()) {
            devices.add(dbObjectToDevice(devicesRecords.next()));
        }
        return devices;
    }

    @Override
    public void register(Device device) {
        Device existingDevice = get(device.getDeviceId());
        if(get(device.getDeviceId()) != null) {
            update(existingDevice);
        } else {
            if (device.getLastUpdate() == null) {
                device.setLastUpdate(new Date());
            }
            if (isBlank(device.getRegistrationId())) {
                device.setRegistrationId(randomUUID().toString());
            }
            devicesCollection().save(deviceToDbObject(device));
        }
    }

    @Override
    public void update(Device device) {
        Device existingDevice = get(device.getDeviceId());
        Map existingDeviceMap = objectMapper.convertValue(existingDevice, Map.class);
        existingDeviceMap.putAll(objectMapper.convertValue(device, Map.class));
        devicesCollection().save(deviceToDbObject(objectMapper.convertValue(existingDeviceMap, Device.class)));
    }

    @Override
    public void deregister(String deviceId) {
        devicesCollection().remove(new BasicDBObject(ImmutableMap.of("deviceId", deviceId)));
    }

    @Override
    public void heartbeat(String deviceId) {
        log.debug("About to send heartbeat to device: {}", deviceId);
        Device device = get(deviceId);
        if(device != null) {
            device.setLastUpdate(new Date());
        }
        devicesCollection().save(deviceToDbObject(device));
    }

    // Helpers

    private DBCollection devicesCollection() {
        return mongo.getDB(db).getCollection(collection);
    }

    private Device dbObjectToDevice(DBObject dbObject) {
        Map<String, Object> deviceMap = new HashMap<>();
        deviceMap.putAll(dbObject.toMap());
        deviceMap.put("id", dbObject.get("_id").toString());
        return objectMapper.convertValue(deviceMap, Device.class);
    }

    private DBObject deviceToDbObject(Device device) {
        Map<String, Object> deviceMap = objectMapper.convertValue(device, Map.class);
        if(device.getId() != null) {
            deviceMap.put("_id", new ObjectId(device.getId()));
        }
        return new BasicDBObject(deviceMap);
    }

}
