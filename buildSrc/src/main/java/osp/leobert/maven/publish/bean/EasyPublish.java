package osp.leobert.maven.publish.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import groovy.lang.Closure;

/**
 * <p><b>Package:</b> osp.leobert.maven.publish.bean </p>
 * <p><b>Classname:</b> EasyPublish </p>
 * Created by leobert on 2021/5/10.
 */
public class EasyPublish {

    public static class Tmp {
        public Object value;
    }

    public static class Developer {
        public String id;
        public String name;
        public String email;

        @Override
        public String toString() {
            return "Developer{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Developer developer = (Developer) o;

            if (!Objects.equals(id, developer.id)) return false;
            if (!Objects.equals(name, developer.name)) return false;
            return Objects.equals(email, developer.email);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (email != null ? email.hashCode() : 0);
            return result;
        }
    }

    public Object sourceSet;

    public String docClassPathAppend;

    public List<String> docExcludes;

    public String groupId;

    public String description;

    public String artifactId;

    public String version;

    public List<Object> artifactsAppend = new ArrayList<>();

    public String packaging;

    public String licenseName;

    public String licenseUrl;

    public List<Developer> mDevelopers = new ArrayList<>();

    public String siteUrl;

    public String gitUrl;

    public String mavenRepoUrl;

    public boolean needSign = true;

    public void developer(Closure closures) {
        if (closures == null) return;
        Developer developer = new Developer();
        org.gradle.util.ConfigureUtil.configure(closures, developer);
        if (!mDevelopers.contains(developer))
            mDevelopers.add(developer);
    }

    public void artifact(Closure closures) {
        if (closures == null) return;
        Tmp artifact = new Tmp();
        org.gradle.util.ConfigureUtil.configure(closures, artifact);
        if (!artifactsAppend.contains(artifact.value))
            artifactsAppend.add(artifact.value);
    }

    public static final String end = "\n";

    @Override
    public String toString() {
        return "EasyPublish{" + end +
                "needSign=" + needSign + end +
                "sourceSet=" + sourceSet + end +
                "description=" + description + end +
                ", docClassPathAppend='" + docClassPathAppend + '\'' + end +
                ", docExcludes=" + docExcludes + end +
                ", groupId='" + groupId + '\'' + end +
                ", artifactId='" + artifactId + '\'' + end +
                ", version='" + version + '\'' + end +
                ", artifactsAppend=" + artifactsAppend + end +
                ", packaging='" + packaging + '\'' + end +
                ", licenseName='" + licenseName + '\'' + end +
                ", licenseUrl='" + licenseUrl + '\'' + end +
                ", developers=" + mDevelopers + end +
                ", siteUrl='" + siteUrl + '\'' + end +
                ", gitUrl='" + gitUrl + '\'' + end +
                ", mavenRepoUrl='" + mavenRepoUrl + '\'' + end +
                '}';
    }
}
