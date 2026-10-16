package com.labourse.matching.dto;

import lombok.Data;

// Pushed by labour-service whenever a profile is created/updated — fills in the Redis hash
// fields that findMatchingLabour() filters on (type, skills, gender, etc). Previously only
// location/availability/kycVerified were ever synced, so "type" and "skills" were always
// empty in Redis — every match silently failed the type-check. This closes that gap.
@Data
public class LabourProfileSyncDto {
    private String type;             // coarse category, display-only — NOT used for matching anymore
    private String serviceCodesCsv;  // catalog ServiceItem codes this labour offers, e.g.
                                      // "HOME_REPAIR__ELECTRICIAN,CLEANING__KITCHEN_CLEANING" —
                                      // THIS is what a job's single requiredServiceCode is matched against
    private String skillsCsv;
    private String subSkillsCsv;
    private String gender;        // "MALE" | "FEMALE"
    private String languagesCsv;  // e.g. "HINDI,ENGLISH"
    private Double rate;          // labour's quoted rate for budget filtering
}
