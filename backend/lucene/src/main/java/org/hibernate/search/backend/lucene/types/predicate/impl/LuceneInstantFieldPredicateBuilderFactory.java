/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneInstantFieldConverter;

public final class LuceneInstantFieldPredicateBuilderFactory
		extends AbstractLuceneStandardFieldPredicateBuilderFactory<LuceneInstantFieldConverter> {

	public LuceneInstantFieldPredicateBuilderFactory(LuceneInstantFieldConverter converter) {
		super( converter );
	}

	@Override
	public LuceneInstantMatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneInstantMatchPredicateBuilder( searchContext, absoluteFieldPath, converter );
	}

	@Override
	public LuceneInstantRangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath) {
		return new LuceneInstantRangePredicateBuilder( searchContext, absoluteFieldPath, converter );
	}
}