package com.spring.teamwork.Service;

import com.spring.teamwork.model.VO.CreditVO;

public interface CreditService {

    CreditVO.Profile getMyProfile(Long userId);

    CreditVO.Profile getOtherProfile(Long targetUserId);
}
