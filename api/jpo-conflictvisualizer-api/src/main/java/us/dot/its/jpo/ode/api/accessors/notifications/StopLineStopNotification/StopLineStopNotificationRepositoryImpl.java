package us.dot.its.jpo.ode.api.accessors.notifications.StopLineStopNotification;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.StopLineStopNotification;

@Component
public class StopLineStopNotificationRepositoryImpl implements StopLineStopNotificationRepository{
    
    @Autowired
    private MongoTemplate mongoTemplate;

    private String collectionName = "CmStopLineStopNotification";


    public Query getQuery(Integer intersectionID, Long startTime, Long endTime, boolean latest){
        Query query = new Query();

        if(intersectionID != null){
            query.addCriteria(Criteria.where("intersectionID").is(intersectionID));
        }

        if(startTime == null){
            startTime = Instant.ofEpochMilli(0).toEpochMilli();
        }
        if(endTime == null){
            endTime = Instant.now().toEpochMilli(); 
        }

        if(latest){
            query.with(Sort.by(Sort.Direction.DESC, "notificationGeneratedAt"));
            query.limit(1);
        }

        query.addCriteria(Criteria.where("notificationGeneratedAt").gte(startTime).lte(endTime));
        return query;
    }

    public long getQueryResultCount(Query query){
        return mongoTemplate.count(query, StopLineStopNotification.class, collectionName);
    }

    public List<StopLineStopNotification> find(Query query) {
        return mongoTemplate.find(query, StopLineStopNotification.class, collectionName);
    }

    @Override
    public void add(StopLineStopNotification item) {
        mongoTemplate.save(item, collectionName);
    }

}