.text
.globl main
main:		# function decl for main
	sw    $ra, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	sw    $fp, 0($sp)	#PUSH
	subu  $sp, $sp, 4
#### Need to make space for locals (and params?)
	addu  $fp, $sp, 0
.text
f:		# function decl for f
	sw    $ra, 0($sp)	#PUSH
	subu  $sp, $sp, 4
	sw    $fp, 0($sp)	#PUSH
	subu  $sp, $sp, 4
#### Need to make space for locals (and params?)
	addu  $fp, $sp, 0
