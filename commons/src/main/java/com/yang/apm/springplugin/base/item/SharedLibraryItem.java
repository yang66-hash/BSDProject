package com.yang.apm.springplugin.base.item;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class SharedLibraryItem {
    public String sharedLibraryName;
    public Set<String> sharedServices;
    public int servicesNum;

    public SharedLibraryItem(String sharedLibraryName){
        this.sharedLibraryName = sharedLibraryName;
        this.sharedServices = new HashSet<>();
        this.servicesNum = 0;
    }
    public void addSvc(String svc1, String svc2){
        this.sharedServices.add(svc1);
        this.sharedServices.add(svc2);
        this.servicesNum = this.sharedServices.size();
    }
}
