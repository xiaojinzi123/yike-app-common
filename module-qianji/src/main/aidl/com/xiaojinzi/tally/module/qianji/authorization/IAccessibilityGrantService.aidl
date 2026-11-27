package com.xiaojinzi.tally.module.qianji.authorization;

interface IAccessibilityGrantService {
    String enableOwnAccessibility(int userId) = 1;
    void destroy() = 16777114;
}
