package com.scms.backend.course.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.scms.backend.course.CourseRefundException;
import org.junit.jupiter.api.Test;

class CourseRefundSearchTests {
	@Test
	void phoneSearchAcceptsLocalInternationalAndFormattedNumbersWithoutTurningNamesIntoEmptyMatches() {
		assertThat(CourseRefundValidation.phoneSearch("090 123 4567")).isEqualTo("901234567");
		assertThat(CourseRefundValidation.phoneSearch("+84 90 123 4567")).isEqualTo("901234567");
		assertThat(CourseRefundValidation.phoneSearch("(090) 123-4567")).isEqualTo("901234567");
		assertThat(CourseRefundValidation.phoneSearch("1234567")).isEqualTo("1234567");
		assertThat(CourseRefundValidation.phoneSearch("Loc Test One")).isNull();
		assertThat(CourseRefundValidation.phoneSearch("MB-100082")).isNull();
		assertThat(CourseRefundValidation.phoneSearch("%_")).isNull();
		assertThat(CourseRefundValidation.phoneSearch("+ ()")).isNull();
	}
	@Test
	void blankSearchDoesNotFilterAndMemberCodesAreTrimmed() {
		assertThat(CourseRefundValidation.search(null)).isNull();
		assertThat(CourseRefundValidation.search(" \t ")).isNull();
		assertThat(CourseRefundValidation.search("  MB-100082  ")).isEqualTo("MB-100082");
		assertThat(CourseRefundValidation.search("Nguyễn Minh An")).isEqualTo("Nguyễn Minh An");
	}

	@Test
	void overlongSearchIsRejectedAndLiteralSearchTextIsPreservedForBoundParameters() {
		assertThat(CourseRefundValidation.search("x".repeat(100))).hasSize(100);
		assertThatThrownBy(() -> CourseRefundValidation.search("x".repeat(101))).isInstanceOf(CourseRefundException.class);
		assertThat(CourseRefundValidation.search("%_'")).isEqualTo("%_'");
	}
}
