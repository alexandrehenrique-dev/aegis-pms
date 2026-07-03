package br.com.byop.aegis.tenant.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TenantMembershipTest {

    @Test
    void shouldCompleteTutorialAndStampTimestamp() {
        TenantMembership membership = new TenantMembership(new Tenant("byop", "BYOP"), "subject", "EDITOR");

        membership.completeTutorial();

        assertThat(membership.isTutorialCompleted()).isTrue();
        assertThat(membership.getTutorialCompletedAt()).isNotNull();
    }

    @Test
    void shouldKeepOriginalTimestampWhenCompletingTutorialTwice() {
        TenantMembership membership = new TenantMembership(new Tenant("byop", "BYOP"), "subject", "EDITOR");
        membership.completeTutorial();
        OffsetDateTime firstCompletion = membership.getTutorialCompletedAt();

        membership.completeTutorial();

        assertThat(membership.getTutorialCompletedAt()).isEqualTo(firstCompletion);
    }

    @Test
    void shouldStartWithTutorialNotCompleted() {
        TenantMembership membership = new TenantMembership(new Tenant("byop", "BYOP"), "subject", "EDITOR");

        assertThat(membership.isTutorialCompleted()).isFalse();
        assertThat(membership.getTutorialCompletedAt()).isNull();
    }
}
