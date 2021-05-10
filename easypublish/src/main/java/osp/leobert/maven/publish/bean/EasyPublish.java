package osp.leobert.maven.publish.bean;

import java.util.List;
import java.util.Objects;

/**
 * <p><b>Package:</b> osp.leobert.maven.publish.bean </p>
 * <p><b>Classname:</b> EasyPublish </p>
 * Created by leobert on 2021/5/10.
 */
public class EasyPublish {
    public Object sourceSet;

    public String docClassPathAppend;

    public List<String> docExcludes;

    @Override
    public String toString() {
        return "EasyPublish{" +
                "sourceSet=" + sourceSet +
                ", docClassPathAppend='" + docClassPathAppend + '\'' +
                ", docExcludes=" + Objects.toString(docExcludes) +
                '}';
    }
}
