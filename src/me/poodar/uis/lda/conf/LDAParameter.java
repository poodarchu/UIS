package me.poodar.uis.lda.conf;

public class LDAParameter {
  public static int K = 15; //topic number
  public static int topNum = 20;  //取最大的20个
  public static float alpha = (float) 0.01; //doc-topic dirichlet prior parameter 
  public static float beta = (float) 0.01;//topic-word dirichlet prior parameter
  public static int iterations = 201;//Times of iterations
  public static int saveStep = 100 ;//The number of iterations between two saving
  public static int beginSaveIters = 100;//Begin save model at this iteration
  
  public static double seedParameter = 0.6;
  
  public LDAParameter() {
    // TODO Auto-generated constructor stub
  }
  
  public void getParameter(String parameterPath) {
    //get parameter from file
  }
}
//基于概率统计的pLSA模型
//每个文档在所有主题上服从多项分布,每个主题在所有词项上服从多项分布
//整个文档的生成过程是:
//1, 以P(di)的概率选中文档di
//2, 以P(zk|di)的概率选中主题zk
//3, 以P(wj|zk)的概率产生一个单词wj

//观察数据为(di, wj)对,主题zk是隐含变量
//假定P(zk|di), P(wj|zk)已知,求隐含变量zk的后验概率
//EM算法
//pLSA应用于信息检索,过滤,自然语言处理等领域,pLSA考虑到词分布和主题分布,使用EM算法来学习参数
//pLSA可以看做概率化得矩阵分解.


//LDA:
//超参数:决定参数的参数
//Symmetric Dirichlet Distribution:对称的狄利克雷分布
//symmetric dirichlet distributions are often used when a Dirichlet prior is called fof, since there typically is no prior
//knowledge favoring one component over another. Since all elements of the parameter vector have the **same value**,
//the distribution alternatively can be parametrized by a single scalar value alpha, called the concentration parameter(聚集参数)
//when parameter alpha  = 1, the sdd is equivalent to a uniform distribution over the open standard (K-1)-simplex, it's uniform over all points in its support.
//values of the concentration parameter above 1 prefer variants that are dense, evenly distributed distributions.

//对LDA的解释:
//1, 共有m篇文章,一共涉及K个主题
//2, 每篇文章(长度为Nm)都有各自的主题分布,主题分布是多项分布,该多项分布的参数(概率分布p)服从Dirichlet分布,该Dirichlet分布的参数为alpha(超参数)
//3, 每个主题都有各自的词分布,词分布为多项分布,该多项分布的参数服从Dirichlet分布,该Dirichlet分布的参数为beta
//4, 对于某篇文章中的第n个词,首先从该文章的主题分布(多项分布)中采样一个主题,然后在这个主题对应的词分布中采样一个词,不断重复这个过程,直到m篇文章全都完成上述过程

//详细解释:
//1, 字典中共有V个term,不可重复,这些term出现在具体的文章中,就是word,而在某篇具体的文章中的word当然是有可能重复的.
//2, 语料库corpus中共有m篇文档d1, d2, d3,...
//3, 对于文档di,由Ni个word组成,可重复
//4, 语料库中共有K个主题T1, T2, T3, ...
//5, alpha 和 beta为先验分布的参数,一般事先给定:如取0.1的对称Dirichlet分布, 表示在参数学习结束后,期望每个文档的主题**不会十分集中**
//6, theta是**每篇文章**的主题分布,第i篇文章的主题分布是thetai= (thetai1,thetai2, thetai3, ... )是长度为K的向量
//7, 对于第i篇文档di, 在主题分布thetai下,可以确定一个具体的主题zij = k
//8, phi 表示第k个主题的词分布,phi_k = (phi_k_1, phi_k_2, ... )是长度为V的向量,V是字典中term的数目
//9, 由zij选择phi_zij,表示由词分布phi_zij确定term,即得到观测值wij
//10, K为主题个数,M为文档总数, Nm为第m个文档的单词总数.两个隐含变量theta和phi分别表示第m篇文档下的topic分布和第k个topic下的词分布,前者是k维,后者是v维


//参数的学习:
//给定一个文档集合,wmn是可以观测到的已知变量,alpha和beta是根据经验给定的先验参数, 其他的变量zmn, theta, phi都是未知的隐含变量,需要根据观察到的变量来学习估计的.

//Gibbs Sampling
//gibbs sampling的运行方式是每次选取概率向量的一个维度, 给定**其他维度**的变量值采样当前维度的值,不断迭代直到收敛输出带估计的参数.
//初始时随机给文本中的每个词分配主题,然后统计每个主题z下出现词t的数量,以及每个文档m下出现主题z的数量
//每一轮计算p(zi|z-i, d, w),即排除当前词的主题分布,根据其他所有词的主题分布估计当前词分配各个主题的概率
//当得到当前词属于所有主题z的概率分布后,根据这个概率分布采样一个新的主题,
//用同样的方法更新下一个词的主题,直到发现每个文档的主题分布theta_i和每个主题的词分布phi_j收敛,算法停止,输出待估计的参数theta和phi,同时每个单词的主题zmn也可同时得出
//实际应用中会设置最大迭代次数
//每一次计算p(zi|z_-i ,d,w)的公式称为Gibbs Sampling rule

//超参数的确定:
//1, 交叉验证
//2, alpha表达了不同文档间主题是否鲜明,beta度量了有多少近义词能够属于同一个类别


//LDA总结:
//由于在此和文档之间加入主题的概念,可以较好地解决一次多义和多词一意的问题
//在实践中发现,LDA用于短文档的效果往往不明显:因为一个词被分配给某个主题的次数和一个主题包括的词数目尚未收敛,往往需要通过其他方案"链接成**长文档**
//LDA可以和其他算法相结合
 //首先,使用LDA将长度为Ni的文档降维到K维
 //同时给出每个主题的概率(主题分布)
 //从而可以使用if-idf继续分析或者直接作为文档的特征进入聚类或者标签传播算法---用于社区发现等




