package com.example.document_service.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.codecs.configuration.CodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import com.example.document_service.model.Document;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;

public class DocumentRepository {

	private final MongoClient mongoClient;
	private final MongoCollection<Document> collection;
	private final GridFSBucket gridFSBucket;

	public DocumentRepository(String mongoUri, String databaseName, String collectionName) {
		CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
		CodecRegistry pojoCodecRegistry = fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				fromProviders(pojoCodecProvider));

		MongoClientSettings settings = MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(mongoUri))
				.codecRegistry(pojoCodecRegistry)
				.build();

		this.mongoClient = MongoClients.create(settings);
		MongoDatabase database = this.mongoClient.getDatabase(databaseName);
		this.collection = database.getCollection(collectionName, Document.class);
		
		// Inizializzazione GridFSBucket per la gestione dei file binari
		this.gridFSBucket = GridFSBuckets.create(database);

		ensureIndexes();
	}

	// getter per GridFSBucket
	public GridFSBucket getGridFSBucket() {
		return gridFSBucket;
	}

	private void ensureIndexes() {
		collection.createIndex(Indexes.ascending("userId"));
		collection.createIndex(Indexes.descending("uploadDate"));
		collection.createIndex(Indexes.descending("lastModified"));
		collection.createIndex(Indexes.compoundIndex(
				Indexes.ascending("userId"),
				Indexes.descending("lastModified")));
	}

	public Document create(Document document) {
		collection.insertOne(document);
		return document;
	}

	public List<Document> findByUserId(long userId) {
		return findByUserId(userId, 0, 20, false);
	}

	public List<Document> findByUserId(long userId, int page, int size, boolean sortAscending) {
		int skip = page * size;
		FindIterable<Document> iterable = collection.find(Filters.eq("userId", userId))
				.sort(sortAscending ? Sorts.ascending("lastModified") : Sorts.descending("lastModified"))
				.skip(skip)
				.limit(size);

		List<Document> documents = new ArrayList<>();
		for (Document document : iterable) {
			documents.add(document);
		}
		return documents;
	}

	public Optional<Document> findByIdAndUserId(String id, long userId) {
		if (!ObjectId.isValid(id)) {
			return Optional.empty();
		}

		Document document = collection.find(Filters.and(
				Filters.eq("_id", new ObjectId(id)),
				Filters.eq("userId", userId)))
				.first();

		return Optional.ofNullable(document);
	}

	public Optional<Document> update(Document document) {
		if (document.getId() == null || !ObjectId.isValid(document.getId())) {
			return Optional.empty();
		}

		var result = collection.replaceOne(
				Filters.and(
						Filters.eq("_id", new ObjectId(document.getId())),
						Filters.eq("userId", document.getUserId())),
				document,
				new ReplaceOptions().upsert(false));

		if (result.getMatchedCount() == 0) {
			return Optional.empty();
		}

		return Optional.of(document);
	}

	public boolean deleteByIdAndUserId(String id, long userId) {
		if (!ObjectId.isValid(id)) {
			return false;
		}

		var result = collection.deleteOne(Filters.and(
				Filters.eq("_id", new ObjectId(id)),
				Filters.eq("userId", userId)));

		return result.getDeletedCount() > 0;
	}

	public long deleteAllByUserId(long userId) {
		var result = collection.deleteMany(Filters.eq("userId", userId));
		return result.getDeletedCount();
	}

	public void close() {
		mongoClient.close();
	}
}