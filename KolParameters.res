# don't use tabs!!
#------- HV.java    ------------
DLEV            0         # debug level
FANCY           0         # animation level -1, 0, +1...

# Good move the bar to the right side edge align the reset eye
WeightFileDir	./
WeightFile	jjb.wgt

#------- Graspable.java -------------
bgColor_R  0
bgColor_G  255
bgColor_B  255

fgColor_R  0
fgColor_G  0
fgColor_B  0

obj_location_PAR_code_len    10
obj_location_MER_code_len    10
obj_location_RAD_code_len    1

obj_axis_MER_code_len        10
obj_axis_PAR_code_len        10
obj_axis_RAD_code_len        1

obj_encode_var               1

visualInput_code_len         10

virtualFingerTarget_code_len 10

affordance_code_len          10

virtualFinger1_code_len      5
virtualFinger2_code_len      5

minPAR        -45          # these are object locations
maxPAR         45
minMER        -45
maxMER         45
minRAD        700
maxRAD       1300

minTILT        0          # these are object axis
maxTILT        90

wrist_rotation_BANK_code_len   9   # the wrist rotations of the hand
wrist_rotation_PITCH_code_len  9   # ""
wrist_rotation_HEADING_code_len 1  # the heading of the hand

hand_position_PAR_code_len     7  # allocentric target hand position
hand_position_MER_code_len     7
hand_position_RAD_code_len     1

finger_FORCE_code_len     10

drawCube   0   # use cube+/- from sim. commandline to toggle
defaultArmHandFile objects/humanhand.xml
homeDir /home/jbonaiuto/documents/School/USC/BrainSimLab/Projects/SME/mns2
gnuplotExecutable   gnuplot
plotDataDir  /tmp
vistaExecutable /home/jbonaiuto/documents/School/USC/BrainSimLab/Projects/SME/ViSTa/vista-5.0.5/vista
saliencyCode /home/jbonaiuto/documents/School/USC/BrainSimLab/Projects/SME/saliency
useMatlab false

motor.DLEV      0         # motor debug level
motor.PLOTLEV   0         # plots shown in each GRASP or WRISTGRASP ?
eta             0.5       # base learning rate
plotLabel       1         # <> shall Motor.java label the axis ?
plotKey         1         # <> shall Gplot axis/ticks/key ?
GVAR            0.75      # this gausball variance  [not effective now]
softVAR         2         # this 1/x where x is the softmax variance
PDFthreshold    0.2       # in generating actions min. req prob.
WTA_ERR	        0.0       # the WTA maybe wrong by this amount (in prob units)

hand_preshape_Randomness     1      # 1=full random 0=from the pdf
reach_target_Randomness      1      # 1=full random 0=from the pdf
wrist_rotation_Randomness    1      # 1=full random 0=from the pdf
hand_position_Randomness     1      # 1=full random 0=from the pdf
finger_force_Randomness      1      # 1=full random 0=from the pdf
virtual_finger_target_Randomness    1       # 1=full random 0=from the pdf
affordance_Randomness        1
virtual_finger_Randomness    1

MAXREACH          30            # after each XX reaches change the input condition
MAXROTATE         25            # after each XX wrist trials, try next obj-off reach
MAXGRASP          30            # after each XX grasp trials, try next wrist rotation reach
MAXBABBLE         200000        # total babbles to be done
weightSave        250           # save after each #weightsave babbles
costThreshold     0.75          # graspCosts  < this are good
negReinforcement  -0.05
palmThreshold     150           # less the distance means palm is close to the object
newton_MAX_IT     1000          #iter. to gradient descend for force balance
slipCost          0.1           # 0 for none 0.1 is pretty high!
Reach2Target     MIDDLE0  # {INDEX,MIDDLE,THUMB}x{0,1,2}   0 is the 1st knuckle 2 is the tip
                         # use THUMBTIP for the tip of the thumb
