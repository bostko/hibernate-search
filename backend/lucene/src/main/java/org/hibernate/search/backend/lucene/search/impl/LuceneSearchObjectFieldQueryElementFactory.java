/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

/**
 * A factory for query elements (predicates, sorts, projections, aggregations, ...) targeting object fields.
 *
 * @param <T> The type returned by {@link #create(LuceneSearchContext, LuceneSearchObjectFieldContext)}.
 * Can be the type of the query element, or an intermediary builder type.
 */
public interface LuceneSearchObjectFieldQueryElementFactory<T> {

	/**
	 * @param searchContext The search context, i.e. information regarding the targeted indexes.
	 * @param field The targeted field.
	 * @return The query element, or an intermediary builder (depending on the factory type).
	 */
	T create(LuceneSearchContext searchContext, LuceneSearchObjectFieldContext field);

	/**
	 * Checks whether this factory and the given factory can be used interchangeably.
	 * @param other Another factory.
	 * @throws org.hibernate.search.util.common.SearchException if the two factories cannot be used interchangeably.
	 */
	void checkCompatibleWith(LuceneSearchObjectFieldQueryElementFactory<?> other);

}
