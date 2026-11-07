package dev.tsukuyomi.libra.engine;

import dev.tsukuyomi.libra.domain.entity.Policy;
import dev.tsukuyomi.libra.domain.enums.JsIdenfier;

public interface IPolicyEngine {

    Boolean evaluate(Policy policy, JsIdenfier identifier, String inputAttrs);

}
