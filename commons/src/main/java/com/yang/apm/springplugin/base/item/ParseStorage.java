package com.yang.apm.springplugin.base.item;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data

//针对每一个类进行信息存储
public class ParseStorage {
    private List<FieldDeclaration> fieldDeclarations;
    private List<MethodDeclaration> methodDeclarations;

    public ParseStorage(){
        fieldDeclarations = new LinkedList<>();
        methodDeclarations = new LinkedList<>();
    }
}
