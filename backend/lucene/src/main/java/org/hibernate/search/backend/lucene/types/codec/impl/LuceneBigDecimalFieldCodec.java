/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;

public final class LuceneBigDecimalFieldCodec extends AbstractLuceneNumericFieldCodec<BigDecimal, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int decimalScale;

	public LuceneBigDecimalFieldCodec(boolean projectable, boolean sortable, BigDecimal indexNullAsValue, int decimalScale) {
		super( projectable, sortable, indexNullAsValue );
		this.decimalScale = decimalScale;
	}

	@Override
	void validate(BigDecimal value) {
		if ( value.compareTo( BigDecimal.valueOf( Long.MAX_VALUE, decimalScale ) ) > 0 ) {
			throw log.bigDecimalTooLarge( value );
		}
	}

	@Override
	void doEncodeForProjection(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, BigDecimal value,
			Long encodedValue) {
		// storing field as String for projections
		documentBuilder.addField( new StoredField( absoluteFieldPath, value.toString() ) );
	}

	@Override
	public BigDecimal decode(Document document, String absoluteFieldPath) {
		IndexableField field = document.getField( absoluteFieldPath );

		if ( field == null ) {
			return null;
		}

		return new BigDecimal( field.stringValue() );
	}

	@Override
	public Long encode(BigDecimal value) {
		return unscale( value );
	}

	@Override
	public LuceneNumericDomain<Long> getDomain() {
		return LuceneNumericDomain.LONG;
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.isCompatibleWith( obj ) ) {
			return false;
		}

		LuceneBigDecimalFieldCodec other = (LuceneBigDecimalFieldCodec) obj;
		return decimalScale == other.decimalScale;
	}

	private Long unscale(BigDecimal value) {
		// See tck.DecimalScaleIT#roundingMode
		return value.setScale( decimalScale, RoundingMode.HALF_UP ).unscaledValue().longValue();
	}
}