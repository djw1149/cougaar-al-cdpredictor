       rC:\Software\DISGroup\Projects\Ultralog\Software\CPESociety\classes\org\cougaar\cpe\model\ReferenceWorldState.class )org.cougaar.cpe.model.ReferenceWorldState  org.cougaar.cpe.model.WorldState ReferenceWorldState.java   !        longRangeSensor :Lorg/cougaar/cpe/model/ReferenceWorldState$StandardSensor;    mediumRangeSensor :Lorg/cougaar/cpe/model/ReferenceWorldState$StandardSensor;    shortRangeSensor :Lorg/cougaar/cpe/model/ReferenceWorldState$StandardSensor;    inactiveTargets Ljava/util/ArrayList;    random Ljava/util/Random;    sensors Ljava/util/ArrayList;       <init> (DDDDD)V        getInactiveTargets ()Ljava/util/Iterator;        filter <(IZZLjava/awt/Shape;)Lorg/cougaar/cpe/model/WorldStateModel;        moveInactiveTargets ()V        updateWorldState ()V        updateSensors ()V           8org.cougaar.cpe.model.ReferenceWorldState$StandardSensor     *org.cougaar.cpe.model.TargetGeneratorModel    <init> (DDDDD)V         updateWorldState ()V         &org.cougaar.cpe.unittests.CPESimulator    <init> (DDDDD)V         filter <(IZZLjava/awt/Shape;)Lorg/cougaar/cpe/model/WorldStateModel;         updateWorldState ()V         5org.cougaar.cpe.model.ReferenceWorldState$SensorModel     3org.cougaar.cpe.simulator.plugin.CPESimulatorPlugin    updateWorldState ()V         filter <(IZZLjava/awt/Shape;)Lorg/cougaar/cpe/model/WorldStateModel;         <init> (DDDDD)V         updateSensors ()V         9org.cougaar.cpe.simulator.plugin.WorldStateExecutorPlugin    <init> (DDDDD)V         %org.cougaar.cpe.unittests.VGSimulator    <init> (DDDDD)V            8org.cougaar.cpe.model.ReferenceWorldState$StandardSensor      "org.cougaar.cpe.model.TargetEntity       org.cougaar.cpe.model.WorldState      "java.lang.IllegalArgumentException      java.lang.StringBuffer      5org.cougaar.cpe.model.ReferenceWorldState$SensorModel      java.util.ArrayList      org.cougaar.cpe.model.Entity      #org.cougaar.cpe.model.TargetContact      java.awt.Shape      %org.cougaar.cpe.model.WorldStateModel      java.util.Random      java.util.Iterator      &org.cougaar.cpe.model.VGWorldConstants      �C:\Software\DISGroup\Projects\Ultralog\Software\CPESociety\classes\org\cougaar\cpe\model\ReferenceWorldState$StandardSensor.class 8org.cougaar.cpe.model.ReferenceWorldState$StandardSensor 5org.cougaar.cpe.model.ReferenceWorldState$SensorModel ReferenceWorldState.java   !        s Ljava/awt/Shape;     yHorizon F     	xMaxError F     	yMaxError F       	 <init> 1(Lorg/cougaar/cpe/model/ReferenceWorldState;FFF)V        <init> A(Lorg/cougaar/cpe/model/ReferenceWorldState;FFFLjava/awt/Shape;)V        getShape ()Ljava/awt/Shape;        setShape (Ljava/awt/Shape;)V        getVisibleContacts ()Ljava/util/Iterator;        	isVisible I(Lorg/cougaar/cpe/model/WorldState;Lorg/cougaar/cpe/model/TargetEntity;)Z        updateVisible %(Lorg/cougaar/cpe/model/WorldState;)V        makeReferenceContact K(Lorg/cougaar/cpe/model/TargetEntity;)Lorg/cougaar/cpe/model/TargetContact;        makeSensedContact L(Lorg/cougaar/cpe/model/TargetContact;)Lorg/cougaar/cpe/model/TargetContact;           )org.cougaar.cpe.model.ReferenceWorldState    <init> 1(Lorg/cougaar/cpe/model/ReferenceWorldState;FFF)V         getVisibleContacts ()Ljava/util/Iterator;         makeSensedContact L(Lorg/cougaar/cpe/model/TargetContact;)Lorg/cougaar/cpe/model/TargetContact;         updateVisible %(Lorg/cougaar/cpe/model/WorldState;)V            "org.cougaar.cpe.model.TargetEntity       org.cougaar.cpe.model.WorldState      org.cougaar.cpe.model.CPEObject      java.util.Collection      5org.cougaar.cpe.model.ReferenceWorldState$SensorModel      java.util.HashMap      org.cougaar.cpe.model.Entity      #org.cougaar.cpe.model.TargetContact      java.awt.Shape      java.util.Iterator      java.util.Random      )org.cougaar.cpe.model.ReferenceWorldState      ~C:\Software\DISGroup\Projects\Ultralog\Software\CPESociety\classes\org\cougaar\cpe\model\ReferenceWorldState$SensorModel.class 5org.cougaar.cpe.model.ReferenceWorldState$SensorModel java.lang.Object ReferenceWorldState.java           contactsByIdMap Ljava/util/HashMap;       <init> ()V         	isVisible I(Lorg/cougaar/cpe/model/WorldState;Lorg/cougaar/cpe/model/TargetEntity;)Z       updateVisible %(Lorg/cougaar/cpe/model/WorldState;)V       getVisibleContacts ()Ljava/util/Iterator;          8org.cougaar.cpe.model.ReferenceWorldState$StandardSensor    <init> ()V         contactsByIdMap Ljava/util/HashMap;     contactsByIdMap Ljava/util/HashMap;     contactsByIdMap Ljava/util/HashMap;     contactsByIdMap Ljava/util/HashMap;     contactsByIdMap Ljava/util/HashMap;     )org.cougaar.cpe.model.ReferenceWorldState        "org.cougaar.cpe.model.TargetEntity       org.cougaar.cpe.model.WorldState      java.lang.Object      java.util.HashMap      java.util.Iterator      )org.cougaar.cpe.model.ReferenceWorldState     