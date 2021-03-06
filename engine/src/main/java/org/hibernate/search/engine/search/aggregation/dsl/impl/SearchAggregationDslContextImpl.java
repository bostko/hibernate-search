/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.aggregation.dsl.impl;

import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationDslContext;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactoryExtension;

public class SearchAggregationDslContextImpl<F extends SearchAggregationBuilderFactory<?>, PDF extends SearchPredicateFactory>
		implements SearchAggregationDslContext<F, PDF> {
	public static <F extends SearchAggregationBuilderFactory<?>, PDF extends SearchPredicateFactory>
			SearchAggregationDslContextImpl root(F builderFactory, PDF predicateFactory) {
		return new SearchAggregationDslContextImpl<>( builderFactory, predicateFactory );
	}

	private final F builderFactory;
	private final PDF predicateFactory;

	private SearchAggregationDslContextImpl(F builderFactory, PDF predicateFactory) {
		this.builderFactory = builderFactory;
		this.predicateFactory = predicateFactory;
	}

	@Override
	public F builderFactory() {
		return builderFactory;
	}

	@Override
	public PDF predicateFactory() {
		return predicateFactory;
	}

	@Override
	public <PDF2 extends SearchPredicateFactory> SearchAggregationDslContext<F, PDF2> withExtendedPredicateFactory(
			SearchPredicateFactoryExtension<PDF2> extension) {
		return new SearchAggregationDslContextImpl<>(
				builderFactory,
				predicateFactory.extension( extension )
		);
	}
}
