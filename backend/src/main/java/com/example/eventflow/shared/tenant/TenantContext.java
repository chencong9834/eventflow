package com.example.eventflow.shared.tenant;

public final class TenantContext {

  private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> TENANT_TYPE = new ThreadLocal<>();
  private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
  private static final ThreadLocal<String> ROLE_CODE = new ThreadLocal<>();

  private TenantContext() {}

  public static void set(Long tenantId, String tenantType, Long userId, String roleCode) {
    TENANT_ID.set(tenantId);
    TENANT_TYPE.set(tenantType);
    USER_ID.set(userId);
    ROLE_CODE.set(roleCode);
  }

  public static Long tenantId() {
    return TENANT_ID.get();
  }

  public static String tenantType() {
    return TENANT_TYPE.get();
  }

  public static Long userId() {
    return USER_ID.get();
  }

  public static String roleCode() {
    return ROLE_CODE.get();
  }

  public static void clear() {
    TENANT_ID.remove();
    TENANT_TYPE.remove();
    USER_ID.remove();
    ROLE_CODE.remove();
  }
}
