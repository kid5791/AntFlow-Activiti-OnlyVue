package org.openoa.base.constant.enums;

import lombok.Getter;

public enum ProcesTypeEnum implements AfEnumBase{
    //third party account
    THIRD_PARTY_ACCOUNT(1, "DSFZH"),
    ;

    @Getter
    private Integer code;

    @Getter
    private String desc;

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    ProcesTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String getDescByCode(Integer code) {
        for (ProcesTypeEnum statusType : ProcesTypeEnum.values()) {
            if (statusType.code.equals(code)) {
                return statusType.desc;
            }
        }
        return null;
    }

    public static Integer getCodeByDesc(String desc) {
        for (ProcesTypeEnum statusType : ProcesTypeEnum.values()) {
            if (statusType.desc.equals(desc)) {
                return statusType.code;
            }
        }
        return null;
    }
}
