package com.smartpm.service;

import com.smartpm.vo.RecycleBinItemVO;

import java.util.List;

public interface RecycleBinService {
    List<RecycleBinItemVO> list();
    Long restore(String type, Long id);
    Long permanentlyDelete(String type, Long id);
}
