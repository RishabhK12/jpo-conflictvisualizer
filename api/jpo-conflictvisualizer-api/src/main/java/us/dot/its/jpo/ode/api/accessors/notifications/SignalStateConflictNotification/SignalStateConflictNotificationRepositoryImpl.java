package us.dot.its.jpo.ode.api.accessors.notifications.SignalStateConflictNotification;


import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.SignalStateConflictNotification;


@Component
public class SignalStateConflictNotificationRepositoryImpl implements SignalStateConflictNotificationRepository{
    
    @Autowired
    private MongoTemplate mongoTemplate;

    private String collectionName = "CmSignalStateConflictNotification";

    public Query getQuery(Integer intersectionID, Long startTime, Long endTime, boolean latest){
        Query query = new Query();

        if(intersectionID != null){
            query.addCriteria(Criteria.where("intersectionID").is(intersectionID));
        }

        Date startTimeDate = new Date(0);
        Date endTimeDate = new Date();

        if (startTime != null) {
            startTimeDate = new Date(startTime);
        }
        if (endTime != null) {
            endTimeDate = new Date(endTime);
        }

        query.addCriteria(Criteria.where("notificationGeneratedAt").gte(startTimeDate).lte(endTimeDate));

        if(latest){
            query.with(Sort.by(Sort.Direction.DESC, "notificationGeneratedAt"));
            query.limit(1);
        }
        
        return query;
    }

    public long getQueryResultCount(Query query){
        query.limit(-1);
        return mongoTemplate.count(query, SignalStateConflictNotification.class, collectionName);
    }

    public List<SignalStateConflictNotification> find(Query query) {
        return mongoTemplate.find(query, SignalStateConflictNotification.class, collectionName);
    }

    @Override
    public void add(SignalStateConflictNotification item) {
        mongoTemplate.save(item, collectionName);
    }

}