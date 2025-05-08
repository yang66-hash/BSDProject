package com.yang.apm.springplugin.services.staticdetect;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.yang.apm.springplugin.base.Enum.DetectableBS;
import com.yang.apm.springplugin.base.context.staticres.CyclicReferenceContext;
import com.yang.apm.springplugin.pojo.AntiPatternItem;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.base.item.DetectionResItem;
import com.yang.apm.springplugin.base.item.RequestItem;
import com.yang.apm.springplugin.factory.FileFactory;
import com.yang.apm.springplugin.pojo.PathMappingServiceItem;
import com.yang.apm.springplugin.services.IDetectConvert;
import com.yang.apm.springplugin.services.db.AntiPatternItemService;
import com.yang.apm.springplugin.services.db.PathMappingService;
import lombok.NoArgsConstructor;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


/**
 *
 */
@Service
@NoArgsConstructor
public class CyclicReferenceService implements IDetectConvert {
    public FileFactory fileFactory;
    public Map<String, Set<String>> extensionAndImplementation;
    public List<String> projectClasses;
    public static final String objectClassName = "java.lang.Object";
    public List<String> projectNames;
    private CyclicReferenceContext context;
    private PathMappingService pathMappingService;

    private AntiPatternItemService antiPatternItemService;

    @Autowired
    public CyclicReferenceService(FileFactory fileFactory,PathMappingService pathMappingService, AntiPatternItemService antiPatternItemService) {
        this.fileFactory=fileFactory;
        this.pathMappingService=pathMappingService;
        this.antiPatternItemService=antiPatternItemService;
    }

    public void resolvePackageName(List<String> pomFilePaths) throws IOException, XmlPullParserException {
        for (String pomFilePath : pomFilePaths) {
            final File pomFile = new File(pomFilePath);
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model mavenModel = mavenReader.read(new FileReader(pomFile));
            String groupId=mavenModel.getParent()==null?mavenModel.getGroupId():mavenModel.getParent().getGroupId();
            if(groupId=="")
                projectNames.add(mavenModel.getArtifactId());
            else if(mavenModel.getArtifactId() =="")
                projectNames.add(groupId);
            else projectNames.add(groupId+"."+mavenModel.getArtifactId());
        }
    }

    private void resolveExtensionAndImplementation(String serviceName, List<String> javaFilePaths) throws FileNotFoundException, ClassNotFoundException {
        TypeSolver typeSolver = new CombinedTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.setConfiguration(new ParserConfiguration().setSymbolResolver(symbolSolver));
        for (String filePath : javaFilePaths) {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
                //get class extension
                String fullClassName = typeDeclaration.getFullyQualifiedName().isPresent() ? (String) typeDeclaration.getFullyQualifiedName().get() : null;
                if (fullClassName != null) {
                    //build a list containing all the full names of classes in this project
                    projectClasses.add(fullClassName);
                }
            }
        }
        for (String filePath : javaFilePaths) {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            for (TypeDeclaration<?> typeDeclaration:cu.getTypes()) {
                //get class extension
                String fullClassName=typeDeclaration.getFullyQualifiedName().isPresent()? (String) typeDeclaration.getFullyQualifiedName().get() :null;
                if (fullClassName!=null){
                    //build a list containing all the full names of classes in this project
                   // projectClasses.add(fullClassName);
                    if(typeDeclaration.getClass().getName().equals("com.github.javaparser.ast.body.ClassOrInterfaceDeclaration")) {
                        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) typeDeclaration;
                        for (ClassOrInterfaceType classOrInterfaceType : classOrInterfaceDeclaration.getExtendedTypes()) {
                            for (String name : projectClasses) {
                                String[] str = name.split("\\.");
                                if (str[str.length - 1].equals(classOrInterfaceType.getName().asString())) {
                                    if (!extensionAndImplementation.containsKey(name)) {

                                        extensionAndImplementation.put(name, new HashSet<>());
                                    }
                                    extensionAndImplementation.get(name).add(fullClassName);
                                    break;
                                }
                            }
                        }
                        for (ClassOrInterfaceType classOrInterfaceType : classOrInterfaceDeclaration.getImplementedTypes()) {
                            for (String name : projectClasses) {
                                String[] str = name.split("\\.");
                                if (str[str.length - 1].equals(classOrInterfaceType.getName().asString())) {
                                    if (!extensionAndImplementation.containsKey(name)) {
                                        extensionAndImplementation.put(name, new HashSet<>());
                                    }
                                    extensionAndImplementation.get(name).add(fullClassName);
                                    break;
                                }
                            }
                        }
                        for (Node node : classOrInterfaceDeclaration.getChildNodes()) {
                            if (node.getClass().getCanonicalName().equals("com.github.javaparser.ast.body.ClassOrInterfaceDeclaration")) {
                                ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) node;
                                for (ClassOrInterfaceType extendClass : c.getExtendedTypes()) {
                                    if (extendClass.getName().asString().equals(classOrInterfaceDeclaration.getName().asString())) {
                                        int length = classOrInterfaceDeclaration.getName().asString().length();
                                        String innerFullName = fullClassName.substring(0, fullClassName.length() - length) + c.getName().asString();
                                        context.addCyclicReference(serviceName, fullClassName, innerFullName);
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public DetectionResItem getCyclicReference(RequestItem request) {
        try{
            QueryWrapper<AntiPatternItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", DetectableBS.CYCLIC_REFERENCES.getValue());
            //相关异味信息
            AntiPatternItem antiPatternItem = antiPatternItemService.getOne(queryWrapper);
            DetectionResItem detectionResItem = convertToResItem(antiPatternItem);

            this.context=new CyclicReferenceContext();
            //待修改
            QueryWrapper<PathMappingServiceItem> serviceQueryWrapper = new QueryWrapper<>();
            serviceQueryWrapper.eq("service_name", request.getServiceName());
            String path = pathMappingService.getOne(serviceQueryWrapper).getPathInLocalRepository();
            //
            List<String> servicesPath = fileFactory.getSubServicePaths(path);
            for(String servicePath: servicesPath) {
                this.extensionAndImplementation=new HashMap<>();
                this.projectClasses=new ArrayList<>();
                this.projectNames=new ArrayList<>();
                String servicesDirectory = new File(servicePath).getAbsolutePath();
                String serviceName = fileFactory.getServiceName(servicePath);
                List<String> javaFilePaths = fileFactory.getJavaFiles(servicesDirectory);
                String packageName = fileFactory.getPackageName(servicesDirectory);
                resolveExtensionAndImplementation(serviceName, javaFilePaths);

                for (String filePath : javaFilePaths) {
                    CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
                    String publicClassDeclared = null;
                    //usually there is only one classOrInterfaceDeclaration in every java file
                    for (TypeDeclaration<?> type : cu.getTypes()) {
                        if (type.isClassOrInterfaceDeclaration()) {
                            if (type.asClassOrInterfaceDeclaration().getFullyQualifiedName().isPresent())
                                publicClassDeclared = type.asClassOrInterfaceDeclaration().getFullyQualifiedName().get();
                        }
                    }
                    if (!extensionAndImplementation.containsKey(publicClassDeclared)) continue;
                    final NodeList<ImportDeclaration> imports = cu.getImports();
                    List<String> importedClassNames = imports.stream().map(ImportDeclaration::getNameAsString)
                            .filter(name -> name.startsWith(packageName)).collect(Collectors.toList());
                    List<String> importedClassesWithStar = new ArrayList<>();
                    //process import statements without *
                    for (String importedClassName : importedClassNames) {
                        if (importedClassName.endsWith("*")) {
                            List<String> starClasses = projectClasses.stream()
                                    .filter(className -> className.startsWith(importedClassName.substring(0, importedClassName.length() - 3)))
                                    .collect(Collectors.toList());
                            importedClassesWithStar.addAll(starClasses);
                        }
                        if (extensionAndImplementation.get(publicClassDeclared).contains(importedClassName)) {
                            context.addCyclicReference(serviceName, publicClassDeclared, importedClassName);
                        }
                    }
                    //process import statements with *
                    for (String importedClassWithStar : importedClassesWithStar) {
                        if (extensionAndImplementation.get(publicClassDeclared).contains(importedClassWithStar)) {
                            context.addCyclicReference(serviceName, publicClassDeclared, importedClassWithStar);
                        }
                    }
                }
            }
            if(!context.getCyclicReference().isEmpty())
                context.setStatus(true);

            addNew2ResItem(context,detectionResItem,request);
            return detectionResItem;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
