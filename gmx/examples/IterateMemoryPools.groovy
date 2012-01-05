import org.helios.gmx.*;


def gmx = Gmx.newInstance();
gmx.beans("java.lang:type=MemoryPool,name=*", {
    println "${it.objectName}:${it.Usage.committed}";
});