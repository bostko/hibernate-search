module hibernate.search.integrationtest.jdk.module {
	exports org.hibernate.search.integrationtest.jdk.module.service;
	requires java.naming;
	requires org.hibernate.search.engine;
	requires org.hibernate.search.mapper.pojo;
	requires org.hibernate.search.mapper.orm;
	requires org.hibernate.search.backend.lucene;
	requires java.persistence;
	requires org.hibernate.orm.core;
	requires lucene.analyzers.common;
}