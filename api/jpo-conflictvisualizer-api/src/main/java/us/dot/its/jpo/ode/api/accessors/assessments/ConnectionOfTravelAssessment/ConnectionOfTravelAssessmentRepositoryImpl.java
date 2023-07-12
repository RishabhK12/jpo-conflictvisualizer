package us.dot.its.jpo.ode.api.accessors.assessments.ConnectionOfTravelAssessment;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.conflictmonitor.monitor.models.assessments.ConnectionOfTravelAssessment;

@Component
public class ConnectionOfTravelAssessmentRepositoryImpl implements ConnectionOfTravelAssessmentRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    private String collectionName = "CmConnectionOfTravelAssessment";

    public Query getQuery(Integer intersectionID, Long startTime, Long endTime, boolean latest) {
        Query query = new Query();

        if (intersectionID != null) {
            query.addCriteria(Criteria.where("intersectionID").is(intersectionID));
        }

        if (startTime == null) {
            startTime = 0L;
        }
        if (endTime == null) {
            endTime = Instant.now().toEpochMilli();
        }

        query.addCriteria(Criteria.where("assessmentGeneratedAt").gte(startTime).lte(endTime));

        if (latest) {
            query.with(Sort.by(Sort.Direction.DESC, "assessmentGeneratedAt"));
            query.limit(1);
        }
        return query;
    }

    public long getQueryResultCount(Query query) {
        return mongoTemplate.count(query, ConnectionOfTravelAssessment.class, collectionName);
    }

    public List<ConnectionOfTravelAssessment> find(Query query) {
        return mongoTemplate.find(query, ConnectionOfTravelAssessment.class, collectionName);
    }

    @Override
    public void add(ConnectionOfTravelAssessment item) {
        mongoTemplate.save(item, collectionName);
    }

}
