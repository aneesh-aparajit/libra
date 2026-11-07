package dev.tsukuyomi.libra.policy;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;

public interface IPolicyEvaluator {

    Boolean evaluate(Policy policy, JsIdenfier identifier, String inputAttrs);

}
