package com.madeeasy.request;

import lombok.Data;

@Data
public class ApprovalRequestDTO {

    private String expenseDetails;  // This will hold the concatenated expense details (as a string)

    private String approveLink;     // The approval link
    private String rejectLink;      // The rejection link

}
