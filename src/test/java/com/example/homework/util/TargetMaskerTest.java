package com.example.homework.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TargetMaskerTest {

  @Test
  void keepsOnlyFirstCharacterAndDomainOfAnEmail() {
    assertThat(TargetMasker.mask("buyer@example.com")).isEqualTo("b***@example.com");
  }

  @Test
  void keepsOnlyLastTwoCharactersOfOtherTargets() {
    assertThat(TargetMasker.mask("+3725551234")).isEqualTo("***34");
  }

  @Test
  void hidesShortOrMissingTargets() {
    assertThat(TargetMasker.mask("1234")).isEqualTo("***");
    assertThat(TargetMasker.mask(null)).isEqualTo("***");
  }
}
